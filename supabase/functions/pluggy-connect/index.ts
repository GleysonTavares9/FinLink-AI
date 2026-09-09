// Supabase Edge Function: pluggy-connect
// Deploy with: npx supabase functions deploy pluggy-connect --project-ref fcwcjnpiavcaaauigulw

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
    const clientId = Deno.env.get("PLUGGY_CLIENT_ID") ?? "7e315bec-93e2-4bf1-b401-992a8162be06";
    const clientSecret = Deno.env.get("PLUGGY_CLIENT_SECRET") ?? "non0No5-Hf0oWgVvLoJjfBQ4EgtK4qPG8iLpszyRQRU";

    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    // Obter payload com seguranca
    let body: any = {};
    try {
      if (req.method === "POST" || req.method === "PUT") {
        body = await req.json();
      }
    } catch (_) {}

    // Obter userId se fornecido
    const authHeader = req.headers.get("Authorization");
    let userId: string | null = body?.userId || body?.user_id || null;

    if (authHeader && authHeader.startsWith("Bearer ") && !userId) {
      try {
        const userClient = createClient(supabaseUrl, Deno.env.get("SUPABASE_ANON_KEY") ?? "", {
          global: { headers: { Authorization: authHeader } },
        });
        const { data: { user } } = await userClient.auth.getUser();
        if (user?.id) userId = user.id;
      } catch (_) {}
    }

    const url = new URL(req.url);
    const action = url.searchParams.get("action") || body.action || "token";

    // Step A: Obter API Key da Pluggy
    const authRes = await fetch("https://api.pluggy.ai/auth", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ clientId, clientSecret }),
    });
    const authData = await authRes.json();
    if (!authRes.ok || !authData.apiKey) {
      throw new Error(authData.message || "Falha ao autenticar com Pluggy");
    }
    const apiKey = authData.apiKey;

    // =========================================================================
    // 1. ACTION: TOKEN -> Gera Connect Token da Pluggy com Webhook configurado
    // =========================================================================
    if (action === "token") {
      const webhookUrl = `${supabaseUrl}/functions/v1/pluggy-connect?action=webhook`;
      const tokenPayload: any = {
        webhookUrl: webhookUrl,
        options: {
          webhookUrl: webhookUrl,
          oauthRedirectUri: "finlink://pluggy-callback",
        },
      };
      if (userId) {
        tokenPayload.clientUserId = userId;
        tokenPayload.options.clientUserId = userId;
      }

      const tokenRes = await fetch("https://api.pluggy.ai/connect_token", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-API-KEY": apiKey,
        },
        body: JSON.stringify(tokenPayload),
      });
      const tokenData = await tokenRes.json();
      if (!tokenRes.ok || !tokenData.accessToken) {
        throw new Error(tokenData.message || "Falha ao criar Connect Token");
      }

      return new Response(JSON.stringify({ accessToken: tokenData.accessToken }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 200,
      });
    }

    // =========================================================================
    // 2. ACTION: WEBHOOK -> Recebe eventos automáticos da Pluggy quando o banco conecta
    // =========================================================================
    if (action === "webhook") {
      const eventType = body?.event || body?.type;
      const incomingItemId = body?.data?.id || body?.data?.item?.id || body?.itemId || body?.item?.id;

      console.log(`[Webhook Pluggy] Recebido evento: ${eventType}, itemId: ${incomingItemId}`);

      if (incomingItemId) {
        try {
          await syncPluggyItem(incomingItemId, apiKey, supabase, userId);
        } catch (e) {
          console.error(`[Webhook Pluggy] Erro ao sincronizar item ${incomingItemId}:`, e);
        }
      }

      return new Response(JSON.stringify({ received: true }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 200,
      });
    }

    // =========================================================================
    // 3. ACTION: SYNC -> Sincroniza contas e transações reais da Pluggy ou do Banco
    // =========================================================================
    if (action === "sync") {
      const itemId = url.searchParams.get("itemId") || body?.itemId;

      // Se itemId for informado e diferente de "sync_all", busca da Pluggy
      if (itemId && itemId !== "sync_all") {
        const syncResult = await syncPluggyItem(itemId, apiKey, supabase, userId);
        return new Response(JSON.stringify({
          success: true,
          ...syncResult
        }), {
          headers: { ...corsHeaders, "Content-Type": "application/json" },
          status: 200,
        });
      }

      // Se itemId for "sync_all" ou omitido, sincroniza todos os items reais ativos da Pluggy
      try {
        const itemsRes = await fetch("https://api.pluggy.ai/items", {
          headers: { "X-API-KEY": apiKey },
        });
        if (itemsRes.ok) {
          const itemsData = await itemsRes.json();
          const itemsList = itemsData.results || [];
          console.log(`[Sync All] Itens encontrados na Pluggy: ${itemsList.length}`);
          for (const it of itemsList) {
            await syncPluggyItem(it.id, apiKey, supabase, userId);
          }
        }
      } catch (e) {
        console.error("[Sync All] Erro ao sincronizar itens da Pluggy:", e);
      }

      // Retorna os dados gravados no Supabase
      const { data: dbAccounts } = await supabase
        .from("open_finance_accounts")
        .select("*")
        .order("connected_at", { ascending: false });

      const { data: dbTransactions } = await supabase
        .from("open_finance_transactions")
        .select("*")
        .order("transaction_date", { ascending: false })
        .limit(100);

      return new Response(JSON.stringify({
        success: true,
        item: null,
        accounts: dbAccounts || [],
        transactions: dbTransactions || [],
      }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 200,
      });
    }

    return new Response(JSON.stringify({ error: "Ação não suportada" }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 400,
    });
  } catch (error: any) {
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 500,
    });
  }
});

// =============================================================================
// Helper: Sincroniza um Item da Pluggy e grava no Supabase Postgres
// =============================================================================
async function syncPluggyItem(itemId: string, apiKey: string, supabase: any, userId: string | null) {
  // 1. Buscar Item na Pluggy
  const itemRes = await fetch(`https://api.pluggy.ai/items/${itemId}`, {
    headers: { "X-API-KEY": apiKey },
  });
  const item = await itemRes.json();

  // 2. Buscar Contas na Pluggy
  const accountsRes = await fetch(`https://api.pluggy.ai/accounts?itemId=${itemId}`, {
    headers: { "X-API-KEY": apiKey },
  });
  const accountsData = await accountsRes.json();
  const rawAccounts = accountsData.results || [];

  const savedAccounts: any[] = [];
  const allTransactions: any[] = [];

  for (const acc of rawAccounts) {
    const accountPayload = {
      id: acc.id,
      user_id: userId,
      item_id: itemId,
      bank_name: item?.connector?.name || acc.name || "Banco Conectado",
      bank_code: String(item?.connector?.id || "000"),
      account_type: acc.subtype || acc.type || "Conta Corrente",
      account_number: acc.number || "0000",
      current_balance: acc.balance || 0,
      credit_limit: acc.creditData?.creditLimit || 0,
      available_credit_limit: acc.creditData?.availableCreditLimit || 0,
      connector_image_url: item?.connector?.imageUrl || null,
      is_connected: true,
      connected_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    };

    try {
      await supabase
        .from("open_finance_accounts")
        .upsert(accountPayload, { onConflict: "id" });
    } catch (e) {
      console.error("Erro ao fazer upsert na conta:", e);
    }
    savedAccounts.push(accountPayload);

    // 3. Buscar transações reais da conta na Pluggy
    try {
      // API v2 com cursor pagination (endpoint /transactions depreciado - HTTP 410)
      let cursor = undefined;
      let txList = [];
      let fetchPage = 0;
      do {
        const txUrl = cursor
          ? `https://api.pluggy.ai/v2/transactions?accountId=${acc.id}&cursor=${cursor}`
          : `https://api.pluggy.ai/v2/transactions?accountId=${acc.id}`;
        const txPageRes = await fetch(txUrl, {
          headers: { "X-API-KEY": apiKey },
        });
        if (!txPageRes.ok) {
          console.error(`[Pluggy v2] Erro HTTP ${txPageRes.status} ao buscar transacoes da conta ${acc.id}`);
          break;
        }
        const txData = await txPageRes.json();
        const page = txData.results || [];
        txList = txList.concat(page);
        cursor = txData.nextCursor || undefined;
        fetchPage++;
        if (fetchPage >= 5) break; // max ~500 transacoes por conta
      } while (cursor);
      console.log(`[Pluggy v2] Conta ${acc.id}: ${txList.length} transacoes encontradas`);

            // Preparar payloads em batch para upsert eficiente
      const txPayloads = txList.map((tx: any) => {
        const rawAmt = tx.amount || 0;
        // Pluggy v2: se type for CREDIT ou categoria indicar receita -> CREDIT
        const isCredit = tx.type === "CREDIT" || (!tx.type && rawAmt > 0);
        return {
          id: tx.id,
          user_id: userId,
          account_id: acc.id,
          description: tx.description || "Transação Bancária",
          amount: Math.abs(rawAmt),
          payment_type: isCredit ? "CREDIT" : "DEBIT",
          transaction_date: tx.date ? tx.date.split("T")[0] : new Date().toISOString().split("T")[0],
          category: tx.category || "Outros",
          created_at: new Date().toISOString(),
        };
      });

      // Upsert em batch de 100 registros (muito mais rápido que um por vez)
      const BATCH_SIZE = 100;
      for (let b = 0; b < txPayloads.length; b += BATCH_SIZE) {
        const batch = txPayloads.slice(b, b + BATCH_SIZE);
        try {
          await supabase
            .from("open_finance_transactions")
            .upsert(batch, { onConflict: "id" });
        } catch (e) {
          console.error(`[Batch] Erro ao upsert batch ${b}-${b + BATCH_SIZE}:`, e);
        }
      }
      allTransactions.push(...txPayloads);

      // Não duplicar em "expenses" - o app lê diretamente de open_finance_transactions com o campo type
      console.log(`[Batch] ${txPayloads.length} transações salvas para conta ${acc.id}`);
      } catch (txErr) {
      console.error(`Erro ao buscar transações da conta ${acc.id}:`, txErr);
    }
  }

  return {
    item,
    accounts: savedAccounts,
    transactions: allTransactions,
  };
}
