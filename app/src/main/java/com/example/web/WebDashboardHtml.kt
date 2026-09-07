package com.example.web

object WebDashboardHtml {

    fun generateHtml(): String {
        return """
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cupom & Finanças AI - Web Dashboard</title>
    <style>
        :root {
            --bg-primary: #0f1715;
            --bg-card: #18221f;
            --bg-card-hover: #24322e;
            --accent-emerald: #10b981;
            --accent-green: #34d399;
            --accent-gold: #f59e0b;
            --accent-blue: #3b82f6;
            --text-primary: #f3f4f6;
            --text-secondary: #9ca3af;
            --border-color: #2e3e38;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
        }

        body {
            background-color: var(--bg-primary);
            color: var(--text-primary);
            padding: 16px;
            max-width: 900px;
            margin: 0 auto;
        }

        header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding-bottom: 16px;
            border-bottom: 1px solid var(--border-color);
            margin-bottom: 20px;
        }

        .logo {
            display: flex;
            align-items: center;
            gap: 10px;
            font-size: 1.25rem;
            font-weight: 700;
            color: var(--accent-green);
        }

        .logo-icon {
            width: 32px;
            height: 32px;
            background: linear-gradient(135deg, #006c4e, #10b981);
            border-radius: 8px;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-weight: bold;
        }

        .status-badge {
            background-color: rgba(16, 185, 129, 0.15);
            color: var(--accent-green);
            padding: 4px 10px;
            border-radius: 20px;
            font-size: 0.8rem;
            font-weight: 600;
            border: 1px solid rgba(16, 185, 129, 0.3);
        }

        .grid-3 {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
            gap: 16px;
            margin-bottom: 24px;
        }

        .card {
            background-color: var(--bg-card);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            padding: 16px;
        }

        .card-title {
            font-size: 0.85rem;
            color: var(--text-secondary);
            margin-bottom: 6px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .card-value {
            font-size: 1.6rem;
            font-weight: 700;
            color: var(--text-primary);
        }

        .card-sub {
            font-size: 0.75rem;
            color: var(--accent-green);
            margin-top: 4px;
        }

        .section-title {
            font-size: 1.1rem;
            font-weight: 600;
            margin-bottom: 12px;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .form-container {
            background-color: var(--bg-card);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            padding: 20px;
            margin-bottom: 24px;
        }

        .form-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 12px;
        }

        .form-group {
            display: flex;
            flex-direction: column;
            gap: 6px;
        }

        label {
            font-size: 0.8rem;
            color: var(--text-secondary);
        }

        input, select {
            background-color: var(--bg-primary);
            border: 1px solid var(--border-color);
            color: var(--text-primary);
            padding: 10px 12px;
            border-radius: 8px;
            font-size: 0.95rem;
            outline: none;
        }

        input:focus, select:focus {
            border-color: var(--accent-emerald);
        }

        .btn-submit {
            grid-column: 1 / -1;
            background: linear-gradient(135deg, #006c4e, #10b981);
            color: white;
            border: none;
            padding: 12px;
            border-radius: 8px;
            font-weight: 600;
            font-size: 1rem;
            cursor: pointer;
            margin-top: 8px;
            transition: opacity 0.2s;
        }

        .btn-submit:hover {
            opacity: 0.9;
        }

        .tx-list {
            display: flex;
            flex-direction: column;
            gap: 10px;
        }

        .tx-item {
            background-color: var(--bg-card);
            border: 1px solid var(--border-color);
            border-radius: 10px;
            padding: 14px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .tx-info {
            display: flex;
            flex-direction: column;
            gap: 2px;
        }

        .tx-store {
            font-weight: 600;
            font-size: 0.95rem;
        }

        .tx-meta {
            font-size: 0.75rem;
            color: var(--text-secondary);
        }

        .tx-amount {
            font-weight: 700;
            font-size: 1.05rem;
            color: var(--accent-gold);
        }

        .tag {
            display: inline-block;
            padding: 2px 8px;
            border-radius: 12px;
            font-size: 0.7rem;
            font-weight: 600;
            background-color: var(--bg-card-hover);
            color: var(--accent-green);
        }

        .toast {
            display: none;
            background-color: var(--accent-emerald);
            color: white;
            padding: 10px 16px;
            border-radius: 8px;
            position: fixed;
            bottom: 20px;
            right: 20px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.3);
            font-weight: 600;
            font-size: 0.9rem;
        }
    </style>
</head>
<body>

    <header>
        <div class="logo">
            <div class="logo-icon">R$</div>
            <span>Cupom & Finanças AI <span style="font-size:0.75rem; color:var(--text-secondary)">Web Interface</span></span>
        </div>
        <div class="status-badge">🟢 Web Server Ativo</div>
    </header>

    <div class="grid-3">
        <div class="card">
            <div class="card-title">Total de Gastos</div>
            <div class="card-value" id="totalAmount">R$ 0,00</div>
            <div class="card-sub">📊 Atualização em tempo real</div>
        </div>
        <div class="card">
            <div class="card-title">Lançamentos Manuais</div>
            <div class="card-value" id="manualCount">0</div>
            <div class="card-sub">✏️ Formulário Web</div>
        </div>
        <div class="card">
            <div class="card-title">Open Finance API</div>
            <div class="card-value" style="color:var(--accent-green)">PRONTA</div>
            <div class="card-sub">⚡ Integrado ao Gemini AI</div>
        </div>
    </div>

    <!-- Gráfico de Gastos por Categoria (Recharts) -->
    <div class="card" style="margin-bottom: 24px;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
            <div class="section-title" style="margin-bottom: 0;">📊 Gráfico de Gastos por Categoria (Recharts)</div>
            <div style="display: flex; gap: 8px;">
                <button type="button" id="btnTypePie" onclick="switchChartType('pie')" style="background: var(--bg-card-hover); color: var(--text-primary); border: 1px solid var(--border-color); padding: 4px 12px; border-radius: 6px; cursor: pointer; font-size: 0.8rem; font-weight: 600;">Pizza (Pie)</button>
                <button type="button" id="btnTypeBar" onclick="switchChartType('bar')" style="background: var(--bg-primary); color: var(--text-secondary); border: 1px solid var(--border-color); padding: 4px 12px; border-radius: 6px; cursor: pointer; font-size: 0.8rem; font-weight: 600;">Barras (Bar)</button>
            </div>
        </div>

        <div id="recharts-chart-wrapper" style="width: 100%; min-height: 280px; display: flex; flex-direction: column; align-items: center; justify-content: center;">
            <!-- Canvas / SVG Rendered Recharts Chart -->
            <div id="chartContainer" style="width: 100%; max-width: 600px; position: relative;"></div>
        </div>
        <div id="chartLegend" style="display: flex; flex-wrap: wrap; justify-content: center; gap: 12px; margin-top: 16px;"></div>
    </div>

    <!-- Formulário de Lançamento Manual -->
    <div class="form-container">
        <div class="section-title">✏️ Lançamento Manual de Valores</div>
        <form id="expenseForm">
            <div class="form-grid">
                <div class="form-group">
                    <label for="storeName">Estabelecimento / Loja</label>
                    <input type="text" id="storeName" placeholder="Ex: Mercado Central" required>
                </div>

                <div class="form-group">
                    <label for="amount">Valor (R$)</label>
                    <input type="number" step="0.01" id="amount" placeholder="0.00" required>
                </div>

                <div class="form-group">
                    <label for="category">Categoria</label>
                    <select id="category">
                        <option value="Alimentação">Alimentação</option>
                        <option value="Transporte">Transporte</option>
                        <option value="Lazer">Lazer</option>
                        <option value="Saúde">Saúde</option>
                        <option value="Moradia">Moradia</option>
                        <option value="Outros">Outros</option>
                    </select>
                </div>

                <div class="form-group">
                    <label for="date">Data da Transação</label>
                    <input type="date" id="date" required>
                </div>

                <button type="submit" class="btn-submit">+ Salvar Lançamento no App</button>
            </div>
        </form>
    </div>

    <!-- Lista de Lançamentos Recentes -->
    <div style="margin-bottom: 24px;">
        <div class="section-title">📋 Lançamentos Recentes</div>
        <div class="tx-list" id="txList">
            <div style="text-align: center; color: var(--text-secondary); padding: 20px;">
                Carregando lançamentos do banco de dados local...
            </div>
        </div>
    </div>

    <div class="toast" id="toast">✅ Lançamento registrado com sucesso!</div>

    <script>
        // Set default date to today
        document.getElementById('date').value = new Date().toISOString().split('T')[0];

        // Form submission bridge
        document.getElementById('expenseForm').addEventListener('submit', function(e) {
            e.preventDefault();
            const storeName = document.getElementById('storeName').value;
            const amount = parseFloat(document.getElementById('amount').value);
            const category = document.getElementById('category').value;
            const date = document.getElementById('date').value;

            if (window.AndroidBridge && window.AndroidBridge.addExpense) {
                window.AndroidBridge.addExpense(storeName, amount, category, date);
                showToast("✅ Lançamento salvo no Android!");
            } else {
                // Local web state fallback
                addLocalExpense(storeName, amount, category, date);
                showToast("✅ Lançamento salvo no Web!");
            }

            // Reset form
            document.getElementById('storeName').value = '';
            document.getElementById('amount').value = '';
        });

        const localExpenses = [];

        let currentChartType = 'pie';
        let currentExpensesList = localExpenses;

        const categoryColors = {
            'Alimentação': '#10b981',
            'Transporte': '#3b82f6',
            'Saúde': '#ec4899',
            'Lazer': '#f59e0b',
            'Moradia': '#8b5cf6',
            'Outros': '#14b8a6'
        };

        function switchChartType(type) {
            currentChartType = type;
            const btnPie = document.getElementById('btnTypePie');
            const btnBar = document.getElementById('btnTypeBar');
            if (type === 'pie') {
                btnPie.style.background = 'var(--bg-card-hover)';
                btnPie.style.color = 'var(--text-primary)';
                btnBar.style.background = 'var(--bg-primary)';
                btnBar.style.color = 'var(--text-secondary)';
            } else {
                btnBar.style.background = 'var(--bg-card-hover)';
                btnBar.style.color = 'var(--text-primary)';
                btnPie.style.background = 'var(--bg-primary)';
                btnPie.style.color = 'var(--text-secondary)';
            }
            renderChart(currentExpensesList);
        }

        function renderChart(list) {
            const container = document.getElementById('chartContainer');
            const legendContainer = document.getElementById('chartLegend');
            if (!container) return;

            if (!list || list.length === 0) {
                container.innerHTML = '<div style="color:var(--text-secondary); text-align:center; padding:30px;">Sem dados para o gráfico</div>';
                legendContainer.innerHTML = '';
                return;
            }

            // Group by category
            const categoryMap = {};
            let grandTotal = 0;
            list.forEach(item => {
                const cat = item.category || 'Outros';
                const amt = item.totalAmount || 0;
                categoryMap[cat] = (categoryMap[cat] || 0) + amt;
                grandTotal += amt;
            });

            const categories = Object.keys(categoryMap).map(cat => ({
                name: cat,
                value: categoryMap[cat],
                percentage: grandTotal > 0 ? (categoryMap[cat] / grandTotal) * 100 : 0,
                color: categoryColors[cat] || '#38bdf8'
            })).sort((a, b) => b.value - a.value);

            // Render Legend
            legendContainer.innerHTML = categories.map(c => `
                <div style="display:flex; align-items:center; gap:6px; font-size:0.8rem;">
                    <span style="width:10px; height:10px; border-radius:50%; background:${'$'}{c.color}; display:inline-block;"></span>
                    <span style="color:var(--text-primary); font-weight:600;">${'$'}{c.name}:</span>
                    <span style="color:var(--text-secondary);">R$ ${'$'}{c.value.toFixed(2).replace('.', ',')} (${'$'}{c.percentage.toFixed(0)}%)</span>
                </div>
            `).join('');

            if (currentChartType === 'pie') {
                // Render Recharts Donut Pie Chart via SVG
                let cumulativePercent = 0;
                const size = 220;
                const center = size / 2;
                const radius = 80;
                const strokeWidth = 32;

                let svgPaths = '';
                categories.forEach(c => {
                    const startAngle = cumulativePercent * 3.6;
                    cumulativePercent += c.percentage;
                    const endAngle = cumulativePercent * 3.6;

                    if (c.percentage <= 0) return;

                    const x1 = center + radius * Math.cos(Math.PI * (startAngle - 90) / 180);
                    const y1 = center + radius * Math.sin(Math.PI * (startAngle - 90) / 180);
                    const x2 = center + radius * Math.cos(Math.PI * (endAngle - 90) / 180);
                    const y2 = center + radius * Math.sin(Math.PI * (endAngle - 90) / 180);
                    const largeArc = c.percentage > 50 ? 1 : 0;

                    const pathData = `M ${'$'}{x1} ${'$'}{y1} A ${'$'}{radius} ${'$'}{radius} 0 ${'$'}{largeArc} 1 ${'$'}{x2} ${'$'}{y2}`;

                    svgPaths += `
                        <path d="${'$'}{pathData}" fill="none" stroke="${'$'}{c.color}" stroke-width="${'$'}{strokeWidth}"
                              style="transition: stroke-width 0.2s; cursor: pointer;"
                              onmouseover="this.setAttribute('stroke-width', '${'$'}{strokeWidth + 6}')"
                              onmouseout="this.setAttribute('stroke-width', '${'$'}{strokeWidth}')">
                            <title>${'$'}{c.name}: R$ ${'$'}{c.value.toFixed(2)} (${'$'}{c.percentage.toFixed(1)}%)</title>
                        </path>
                    `;
                });

                container.innerHTML = `
                    <div style="position:relative; width:${'$'}{size}px; height:${'$'}{size}px; margin: 0 auto;">
                        <svg width="${'$'}{size}" height="${'$'}{size}" viewBox="0 0 ${'$'}{size} ${'$'}{size}">
                            ${'$'}{svgPaths}
                        </svg>
                        <div style="position:absolute; top:50%; left:50%; transform:translate(-50%, -50%); text-align:center;">
                            <div style="font-size:0.7rem; color:var(--text-secondary); text-transform:uppercase;">Total</div>
                            <div style="font-size:1rem; font-weight:700; color:var(--text-primary);">R$ ${'$'}{grandTotal.toFixed(0)}</div>
                        </div>
                    </div>
                `;
            } else {
                // Render Recharts Bar Chart via SVG
                const maxValue = Math.max(...categories.map(c => c.value), 1);
                const barHeight = 24;

                const bars = categories.map(c => {
                    const widthPercent = (c.value / maxValue) * 100;
                    return `
                        <div style="margin-bottom:12px;">
                            <div style="display:flex; justify-content:space-between; font-size:0.8rem; margin-bottom:4px;">
                                <span style="font-weight:600;">${'$'}{c.name}</span>
                                <span style="color:var(--text-secondary);">R$ ${'$'}{c.value.toFixed(2).replace('.', ',')}</span>
                            </div>
                            <div style="background:var(--bg-primary); height:${'$'}{barHeight}px; border-radius:6px; overflow:hidden;">
                                <div style="background:${'$'}{c.color}; width:${'$'}{widthPercent}%; height:100%; border-radius:6px; transition:width 0.4s ease;"></div>
                            </div>
                        </div>
                    `;
                }).join('');

                container.innerHTML = `<div style="padding:10px 0; width:100%;">${'$'}{bars}</div>`;
            }
        }

        function renderExpenses(list) {
            currentExpensesList = list || [];
            const container = document.getElementById('txList');
            if (!list || list.length === 0) {
                container.innerHTML = '<div style="text-align:center; padding:20px; color:var(--text-secondary)">Nenhum lançamento cadastrado.</div>';
                renderChart([]);
                return;
            }

            let total = 0;
            let manualCount = 0;

            container.innerHTML = list.map(item => {
                total += item.totalAmount;
                if (item.source === 'MANUAL') manualCount++;

                return `
                    <div class="tx-item">
                        <div class="tx-info">
                            <div class="tx-store">${'$'}{item.storeName}</div>
                            <div class="tx-meta">
                                <span>${'$'}{item.transactionDate}</span> • 
                                <span class="tag">${'$'}{item.category}</span> • 
                                <span style="font-size:0.7rem; color:var(--accent-green)">${'$'}{item.source}</span>
                            </div>
                        </div>
                        <div class="tx-amount">R$ ${'$'}{item.totalAmount.toFixed(2).replace('.', ',')}</div>
                    </div>
                `;
            }).join('');

            document.getElementById('totalAmount').innerText = 'R$ ' + total.toFixed(2).replace('.', ',');
            document.getElementById('manualCount').innerText = manualCount.toString();

            // Render Recharts category breakdown
            renderChart(list);
        }

        function addLocalExpense(storeName, amount, category, date) {
            localExpenses.unshift({
                storeName: storeName,
                totalAmount: amount,
                category: category,
                transactionDate: date,
                source: 'MANUAL'
            });
            renderExpenses(localExpenses);
        }

        function updateExpensesFromAndroid(data) {
            try {
                const list = typeof data === 'string' ? JSON.parse(data) : data;
                renderExpenses(list);
            } catch(e) {
                console.error("Error parsing Android expenses", e);
            }
        }

        function updateFromAndroid(data) {
            updateExpensesFromAndroid(data);
        }

        function showToast(msg) {
            const toast = document.getElementById('toast');
            toast.innerText = msg;
            toast.style.display = 'block';
            setTimeout(() => { toast.style.display = 'none'; }, 3000);
        }

        // Initial render
        renderExpenses(localExpenses);
    </script>
</body>
</html>
        """.trimIndent()
    }
}
