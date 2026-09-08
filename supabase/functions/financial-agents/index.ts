// Supabase Edge Function: financial-agents
// Multi-Agent Architecture: Albert (Triage/Anomalies), Sherlock (Categorization/80% Limit), Galileu (Monthly Strategist), and Custom No-Code Agents

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    const geminiApiKey = Deno.env.get("GEMINI_API_KEY") ?? "";

    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    // Obter autenticacao do usuario
    const authHeader = req.headers.get("Authorization");
    let userId: string | null = null;
    if (authHeader) {
      const userClient = createClient(supabaseUrl, Deno.env.get("SUPABASE_ANON_KEY") ?? "", {
        global: { headers: { Authorization: authHeader } },
      });
      const { data: { user } } = await userClient.auth.getUser();
      userId = user?.id ?? null;
    }

    const body = await req.json();
    const action = body.action || "status";
    const targetUserId = body.user_id || userId;

    if (!targetUserId && action !== "status") {
      return new Response(JSON.stringify({ error: "Usuario nao identificado" }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 401,
      });
    }

    // =========================================================================
    // 1. AGENTE ALBERT (Triage Diaria, Cobrancas Duplicadas & Anomalias)
    // =========================================================================
    if (action === "albert_triage") {
      // Buscar transacoes dos ultimos 30 dias
      const { data: txs, error: txErr } = await supabase
        .from("open_finance_transactions")
        .select("*")
        .eq("user_id", targetUserId)
        .order("transaction_date", { ascending: false })
        .limit(100);

      if (txErr) throw txErr;

      const alertsCreated: any[] = [];
      const transactions = txs || [];

      // A) Deteccao de Cobranca Duplicada (< 24h, mesmo valor e mesma descricao)
      const duplicatePairs: any[] = [];
      for (let i = 0; i < transactions.length; i++) {
        for (let j = i + 1; j < transactions.length; j++) {
          const t1 = transactions[i];
          const t2 = transactions[j];
          if (
            Math.abs(t1.amount) === Math.abs(t2.amount) &&
            t1.description.trim().toLowerCase() === t2.description.trim().toLowerCase()
          ) {
            const d1 = new Date(t1.transaction_date).getTime();
            const d2 = new Date(t2.transaction_date).getTime();
            const diffHours = Math.abs(d1 - d2) / (1000 * 60 * 60);
            if (diffHours <= 36) {
              duplicatePairs.push({ t1, t2, diffHours });
            }
          }
        }
      }

      for (const dup of duplicatePairs.slice(0, 3)) {
        const notif = {
          user_id: targetUserId,
          agent_name: "albert",
          title: "Possivel Cobranca Duplicada",
          message: `O Agente Albert detectou 2 cobrancas identicas de R$ ${Math.abs(dup.t1.amount).toFixed(2)} em '${dup.t1.description}'. Verifique se houve duplicidade no seu cartao/conta.`,
          severity: "warning",
          metadata: { t1_id: dup.t1.id, t2_id: dup.t2.id, amount: dup.t1.amount },
        };
        await supabase.from("agent_notifications").insert(notif);
        alertsCreated.push(notif);
      }

      // B) Deteccao de Anomalia de Valor (Gasto > 2.5x da media da categoria)
      const categoryTotals: Record<string, number[]> = {};
      transactions.forEach((t) => {
        const cat = t.category || "Outros";
        if (!categoryTotals[cat]) categoryTotals[cat] = [];
        categoryTotals[cat].push(Math.abs(t.amount));
      });

      for (const [cat, amounts] of Object.entries(categoryTotals)) {
        if (amounts.length >= 3) {
          const avg = amounts.reduce((a, b) => a + b, 0) / amounts.length;
          const maxVal = Math.max(...amounts);
          if (maxVal > avg * 2.5 && maxVal > 150) {
            const outT = transactions.find((t) => Math.abs(t.amount) === maxVal && (t.category || "Outros") === cat);
            if (outT) {
              const notif = {
                user_id: targetUserId,
                agent_name: "albert",
                title: `Gasto Anomalo em ${cat}`,
                message: `O Agente Albert observou um gasto atipico de R$ ${maxVal.toFixed(2)} em '${outT.description}'. Sua media nessa categoria e de R$ ${avg.toFixed(2)}.`,
                severity: "info",
                metadata: { tx_id: outT.id, amount: maxVal, avg },
              };
              await supabase.from("agent_notifications").insert(notif);
              alertsCreated.push(notif);
            }
          }
        }
      }

      return new Response(JSON.stringify({ success: true, alerts: alertsCreated }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 200,
      });
    }

    // =========================================================================
    // 2. AGENTE SHERLOCK (Categorizacao Semantica & Monitor de Limite 80%)
    // =========================================================================
    if (action === "sherlock_categorize") {
      const description = body.description || "";
      const amount = body.amount || 0;

      let categorizedAs = "Outros";
      if (geminiApiKey && description) {
        try {
          const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${geminiApiKey}`;
          const prompt = `Classifique a seguinte despesa bancaria em uma categoria financeira unica e amigavel (ex: Alimentacao/Delivery, Transporte/Uber, Moradia, Saude, Lazer, Assinaturas/Streaming, Educacao, Supermercado, Compras). Responda APENAS com o nome da categoria. Descricao: "${description}"`;
          const gRes = await fetch(geminiUrl, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }] }),
          });
          const gData = await gRes.json();
          categorizedAs = gData.candidates?.[0]?.content?.parts?.[0]?.text?.trim() || "Outros";
        } catch (_) {}
      }

      // Verificar regra de 80% do teto orcamentario
      const { data: limitData } = await supabase
        .from("budget_limits")
        .select("*")
        .eq("user_id", targetUserId)
        .ilike("category", `%${categorizedAs}%`)
        .maybeSingle();

      let thresholdAlert: any = null;
      if (limitData) {
        // Calcular soma do mes nesta categoria
        const startOfMonth = new Date();
        startOfMonth.setDate(1);
        startOfMonth.setHours(0, 0, 0, 0);

        const { data: monthTxs } = await supabase
          .from("open_finance_transactions")
          .select("amount")
          .eq("user_id", targetUserId)
          .gte("transaction_date", startOfMonth.toISOString().split("T")[0]);

        const totalSpent = (monthTxs || []).reduce((acc, cur) => acc + Math.abs(cur.amount), 0) + Math.abs(amount);
        const limitVal = Number(limitData.monthly_limit);
        const percent = (totalSpent / limitVal) * 100;

        if (percent >= (limitData.alert_threshold_percentage || 80)) {
          thresholdAlert = {
            user_id: targetUserId,
            agent_name: "sherlock",
            title: `Alerta de Limite: ${categorizedAs}`,
            message: `O Agente Sherlock identificou que voce atingiu ${percent.toFixed(0)}% do limite estipulado para ${categorizedAs} (R$ ${totalSpent.toFixed(2)} de R$ ${limitVal.toFixed(2)}).`,
            severity: percent >= 100 ? "critical" : "warning",
            metadata: { category: categorizedAs, spent: totalSpent, limit: limitVal, percent },
          };
          await supabase.from("agent_notifications").insert(thresholdAlert);
        }
      }

      return new Response(JSON.stringify({ success: true, category: categorizedAs, alert: thresholdAlert }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 200,
      });
    }

    // =========================================================================
    // 3. AGENTE GALILEU (Estrategista Mensal & Relatorio de Tendencias)
    // =========================================================================
    if (action === "galileu_monthly") {
      const now = new Date();
      const monthYear = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;

      const { data: txs } = await supabase
        .from("open_finance_transactions")
        .select("*")
        .eq("user_id", targetUserId)
        .order("transaction_date", { ascending: false })
        .limit(200);

      const transactions = txs || [];
      const totalExpenses = transactions.reduce((sum, t) => sum + Math.abs(t.amount), 0);

      let reportMarkdown = `### Relatorio Financeiro Mensal (${monthYear})\n\nO Agente Galileu analisou suas transacoes deste mes. Total consolidado de gastos: **R$ ${totalExpenses.toFixed(2)}**.\n\n- **Recomendacao de Aporte:** Priorize manter sua reserva de emergencia antes de novos aportes.\n- **Oportunidade:** Fique atento aos gastos de fim de semana para aumentar sua taxa de poupanca.`;

      if (geminiApiKey && transactions.length > 0) {
        try {
          const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${geminiApiKey}`;
          const prompt = `Atue como o Agente Galileu, um consultor financeiro pessoal experiente, acolhedor e altamente analitico. Analise o seguinte resumo de gastos do usuario (Total de gastos: R$ ${totalExpenses.toFixed(2)}, total de transacoes: ${transactions.length}) e gere um relatorio executivo em Markdown com: 1. Diagnostico do Mes, 2. Principais Tendencias de Gastos, 3. Sugestoes Praticas de Economia e Rebalanceamento de Investimentos. Seja conciso e direto.`;
          const gRes = await fetch(geminiUrl, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }] }),
          });
          const gData = await gRes.json();
          reportMarkdown = gData.candidates?.[0]?.content?.parts?.[0]?.text || reportMarkdown;
        } catch (_) {}
      }

      // Upsert no banco
      const { data: reportSaved } = await supabase
        .from("monthly_strategic_reports")
        .upsert(
          {
            user_id: targetUserId,
            month_year: monthYear,
            total_expenses: totalExpenses,
            report_markdown: reportMarkdown,
          },
          { onConflict: "user_id,month_year" }
        )
        .select()
        .single();

      // Notificar usuario
      await supabase.from("agent_notifications").insert({
        user_id: targetUserId,
        agent_name: "galileu",
        title: "Relatorio Estrategico Mensal Disponivel",
        message: `O Agente Galileu concluiu o fechamento analitico de ${monthYear}. Toque para ler os insights e sugestoes de rebalanceamento.`,
        severity: "info",
        metadata: { month_year: monthYear },
      });

      return new Response(JSON.stringify({ success: true, report: reportSaved }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 200,
      });
    }

    // =========================================================================
    // 4. AGENTES PERSONALIZADOS (No-Code: Frase Natural -> JSON -> Engine)
    // =========================================================================
    if (action === "build_custom_agent") {
      const naturalPrompt = body.prompt || "";
      if (!naturalPrompt) {
        return new Response(JSON.stringify({ error: "Prompt nao fornecido" }), { status: 400 });
      }

      let parsedRule = {
        name: "Monitoramento Customizado",
        description: naturalPrompt,
        metric: "expense_amount",
        operator: ">",
        threshold: 200,
        category: "Geral",
        action: "notify_app",
      };

      if (geminiApiKey) {
        try {
          const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${geminiApiKey}`;
          const parsePrompt = `Transforme a seguinte solicitacao de monitoramento financeiro pessoal em um JSON estruturado exato para um motor de regras de IA:
Frase do usuario: "${naturalPrompt}"

Responda UNICAMENTE com o JSON valido no seguinte formato:
{
  "name": "Nome curto do agente",
  "description": "Descricao simples",
  "metric": "expense_amount" ou "category_sum" ou "usd_price",
  "category": "nome da categoria ou null",
  "operator": ">" ou "<" ou "==" ou ">=",
  "threshold": numero_flutuante,
  "action": "notify_app"
}`;

          const gRes = await fetch(geminiUrl, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ contents: [{ parts: [{ text: parsePrompt }] }] }),
          });
          const gData = await gRes.json();
          const rawText = gData.candidates?.[0]?.content?.parts?.[0]?.text?.trim() || "";
          const jsonMatch = rawText.match(/\{.*\}/s);
          if (jsonMatch) {
            parsedRule = JSON.parse(jsonMatch[0]);
          }
        } catch (_) {}
      }

      const { data: newAgent, error: agentErr } = await supabase
        .from("custom_agents")
        .insert({
          user_id: targetUserId,
          name: parsedRule.name || "Agente Personalizado",
          description: parsedRule.description || naturalPrompt,
          natural_prompt: naturalPrompt,
          rule_json: parsedRule,
          is_active: true,
        })
        .select()
        .single();

      if (agentErr) throw agentErr;

      return new Response(JSON.stringify({ success: true, agent: newAgent }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 200,
      });
    }

    return new Response(JSON.stringify({ status: "Financial Agents Engine Online" }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 200,
    });
  } catch (err: any) {
    return new Response(JSON.stringify({ error: err.message }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 500,
    });
  }
});
