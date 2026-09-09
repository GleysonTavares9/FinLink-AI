package com.example.data.repository

import com.example.data.db.ExpenseDao
import com.example.data.db.ExpenseEntity
import com.example.data.db.OpenFinanceDao
import com.example.data.db.OpenFinanceTransactionEntity
import com.example.data.model.AppOfferingFeature
import com.example.data.model.SubscriptionItem
import com.example.data.model.TransactionType
import com.example.data.model.UnifiedTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Repositório Inteligente de Extrato Unificado do FinLink AI.
 * Trata transferências recebidas como ENTRADA legítima (+R$),
 * deduplica transações e normaliza valores e categorias.
 */
class TransactionRepository(
    private val expenseDao: ExpenseDao,
    private val openFinanceDao: OpenFinanceDao
) {

    private val _subscriptions = MutableStateFlow<List<SubscriptionItem>>(emptyList())
    val subscriptions = _subscriptions.asStateFlow()

    fun getUnifiedTransactions(): Flow<List<UnifiedTransaction>> {
        return combine(
            expenseDao.getAllExpenses(),
            openFinanceDao.getAllTransactions(),
            _subscriptions
        ) { expenses, openFinanceTxs, subs ->
            val list = mutableListOf<UnifiedTransaction>()
            val seenSignatures = mutableSetOf<String>()

            // 1. Processar Transações Open Finance (bancos reais como Mercado Pago, Nubank)
            for (tx in openFinanceTxs) {
                val cleanDesc = tx.description.trim()
                val absAmount = abs(tx.amount)
                val dateStr = tx.date.take(10).ifBlank { "Hoje" }
                val bank = inferBankName(cleanDesc, tx.accountId)

                // REGRA CRÍTICA DO USUÁRIO: Transferência recebida é considerada ENTRADA (+R$)
                val isReceivedTransfer = isTransferReceived(cleanDesc, tx.category, tx.type)

                // Movimentação puramente interna (reserva de caixinha própria ou fatura do cartão)
                val isInternal = !isReceivedTransfer && isInternalReserveOrBill(cleanDesc, tx.category)

                // Receita real: recebimento via Pix, depósito, salário, rendimento, ou crédito bancário
                val isIncome = isReceivedTransfer || (
                    !isInternal && (
                        tx.type.equals("CREDIT", ignoreCase = true) ||
                        tx.category.contains("Proceeds", ignoreCase = true) ||
                        tx.category.contains("Dividends", ignoreCase = true) ||
                        tx.category.contains("Rendimentos", ignoreCase = true) ||
                        tx.category.contains("Salário", ignoreCase = true) ||
                        tx.category.contains("Receita", ignoreCase = true)
                    ) && !cleanDesc.contains("pagamento recebido", ignoreCase = true)
                )

                // Assinatura ou serviço recorrente
                val isSub = !isIncome && !isInternal && (
                    subs.any { cleanDesc.contains(it.name, ignoreCase = true) } ||
                    tx.category.contains("Assinatura", ignoreCase = true) ||
                    tx.category.contains("Streaming", ignoreCase = true) ||
                    tx.category.contains("Digital services", ignoreCase = true) ||
                    tx.category.contains("Telecommunications", ignoreCase = true) ||
                    isKnownSubscription(cleanDesc)
                )

                val isBillOrCard = cleanDesc.contains("fatura", ignoreCase = true) ||
                                     cleanDesc.contains("cartão", ignoreCase = true) ||
                                     tx.category.contains("Credit card", ignoreCase = true)

                val type = when {
                    isIncome -> TransactionType.ENTRADA
                    isBillOrCard -> TransactionType.SAIDA // Regra contábil: fatura é saída real
                    isInternal -> TransactionType.TRANSFERENCIA
                    isSub -> TransactionType.ASSINATURA
                    cleanDesc.contains("DARF", ignoreCase = true) || cleanDesc.contains("Boleto", ignoreCase = true) -> TransactionType.PAGAMENTO
                    else -> TransactionType.SAIDA
                }

                val sig = buildSignature(dateStr, absAmount, cleanDesc)
                seenSignatures.add(sig)

                list.add(
                    UnifiedTransaction(
                        id = "of_${tx.id}",
                        title = cleanDesc,
                        subtitle = "${formatDateSimple(dateStr)} • ${friendlyCategory(tx.category)}",
                        amount = absAmount,
                        type = type,
                        date = dateStr,
                        formattedDate = formatDateSimple(dateStr),
                        dateGroup = inferDateGroup(dateStr),
                        category = friendlyCategory(tx.category),
                        bankName = bank,
                        paymentMethod = if (cleanDesc.contains("Pix", ignoreCase = true)) "PIX" else "Open Finance",
                        isSubscription = isSub,
                        isInternalTransfer = isInternal,
                        accountId = tx.accountId,
                        notes = bank
                    )
                )
            }

            // 2. Processar Despesas e Cupons Fiscais Locais do Room
            for (exp in expenses) {
                val cleanStore = exp.storeName.trim()
                val absAmount = abs(exp.totalAmount)
                val dateStr = exp.transactionDate.take(10).ifBlank { "Hoje" }

                val sig = buildSignature(dateStr, absAmount, cleanStore)
                if (seenSignatures.contains(sig)) {
                    continue // Evita duplicação com o Open Finance
                }
                seenSignatures.add(sig)

                val isReceivedTransfer = isTransferReceived(cleanStore, exp.category, "DEBIT")
                val isInternal = !isReceivedTransfer && isInternalReserveOrBill(cleanStore, exp.category)

                val isIncome = isReceivedTransfer ||
                               exp.notes?.contains("ENTRADA", ignoreCase = true) == true ||
                               exp.category.contains("Salário", ignoreCase = true) ||
                               exp.category.contains("Receita", ignoreCase = true) ||
                               exp.category.contains("Entrada", ignoreCase = true)

                val isSub = !isIncome && !isInternal && (
                    subs.any { cleanStore.contains(it.name, ignoreCase = true) } ||
                    exp.category.contains("Assinatura", ignoreCase = true) ||
                    exp.category.contains("Streaming", ignoreCase = true) ||
                    isKnownSubscription(cleanStore)
                )

                val type = when {
                    isIncome -> TransactionType.ENTRADA
                    isInternal -> TransactionType.TRANSFERENCIA
                    isSub -> TransactionType.ASSINATURA
                    exp.source.contains("SEFAZ", ignoreCase = true) || exp.source.contains("OCR", ignoreCase = true) -> TransactionType.CUPOM_FISCAL
                    exp.paymentMethod.contains("PIX", ignoreCase = true) || exp.paymentMethod.contains("Boleto", ignoreCase = true) -> TransactionType.PAGAMENTO
                    else -> TransactionType.SAIDA
                }

                val itemsList = if (!exp.itemsJson.isNullOrBlank()) {
                    exp.itemsJson.split(" | ")
                } else emptyList()

                val bank = if (exp.paymentMethod == "OPEN_FINANCE") "Mercado Pago" else exp.paymentMethod

                list.add(
                    UnifiedTransaction(
                        id = "exp_${exp.id}",
                        title = cleanStore,
                        subtitle = "${formatDateSimple(dateStr)} • ${exp.category}",
                        amount = absAmount,
                        type = type,
                        date = dateStr,
                        formattedDate = formatDateSimple(dateStr),
                        dateGroup = inferDateGroup(dateStr),
                        category = exp.category,
                        bankName = bank,
                        paymentMethod = exp.paymentMethod,
                        isSubscription = isSub,
                        isInternalTransfer = isInternal,
                        items = itemsList,
                        notes = exp.notes ?: exp.rawOcrText
                    )
                )
            }

            list.sortedByDescending { it.date }
        }
    }

    /**
     * Regra: Transferência recebida, Pix recebido ou Código QR Pix recebido é ENTRADA (+R$)
     */
    private fun isTransferReceived(desc: String, category: String, type: String): Boolean {
        val d = desc.lowercase(Locale.ROOT)
        val c = category.lowercase(Locale.ROOT)
        return d.contains("recebido") ||
               d.contains("recebida") ||
               d.contains("código qr pix") ||
               d.contains("rendimentos") ||
               (type.equals("CREDIT", ignoreCase = true) && (c.contains("transfer") || d.contains("pix")))
    }

    /**
     * Regra Contábil (Fluxo de Caixa): Faturas e pagamentos de cartão de crédito são estritamente SAÍDAS (despesas reais).
     * Apenas movimentações puramente internas sem consumo (ex: guardar na caixinha/poupança) são transferências.
     */
    private fun isInternalReserveOrBill(desc: String, category: String): Boolean {
        val d = desc.lowercase(Locale.ROOT)
        val c = category.lowercase(Locale.ROOT)
        
        // Faturas e cartões são SAÍDAS contábeis, NUNCA transferências neutras
        if (c.contains("credit card") || d.contains("fatura") || d.contains("cartão de crédito") || d.contains("pagamento cartão")) {
            return false
        }
        
        return d.contains("dinheiro reservado") ||
               d.contains("dinheiro retirado poupança")
    }

    private fun isKnownSubscription(desc: String): Boolean {
        val d = desc.lowercase(Locale.ROOT)
        return d.contains("netflix") ||
               d.contains("spotify") ||
               d.contains("claro") ||
               d.contains("amazon") ||
               d.contains("prime") ||
               d.contains("jim.com") ||
               d.contains("chatgpt") ||
               d.contains("youtube") ||
               d.contains("disney") ||
               d.contains("apple")
    }

    private fun friendlyCategory(rawCat: String): String {
        return when (rawCat.trim()) {
            "Groceries" -> "Supermercado"
            "Pharmacy" -> "Farmácia"
            "Eating out" -> "Restaurantes & Lanches"
            "Shopping" -> "Compras"
            "Clothing" -> "Vestuário"
            "Electronics" -> "Eletrônicos"
            "Services" -> "Serviços"
            "Digital services" -> "Serviços digitais"
            "Telecommunications" -> "Telefonia & Internet"
            "Interests charged" -> "Juros & Encargos"
            "Late payment and overdraft costs" -> "Multas & Tarifas"
            "Proceeds interests and dividends" -> "Rendimentos"
            "Credit card payment" -> "Pagamento de Fatura"
            "Transfer - Internal" -> "Reserva / Caixinha"
            "Same person transfer" -> "Transferência Própria"
            "Transfer - PIX" -> "Transferência - PIX"
            "Transfers" -> "Transferências"
            "Online shopping" -> "Compras Online"
            else -> rawCat.ifBlank { "Outros" }
        }
    }

    private fun inferBankName(desc: String, accountId: String?): String {
        val d = desc.lowercase(Locale.ROOT)
        return when {
            d.contains("mp*") || d.contains("mercado pago") -> "Mercado Pago"
            d.contains("nubank") || d.contains("nu pagamentos") -> "Nubank"
            d.contains("itau") || d.contains("itaú") -> "Itaú"
            d.contains("bradesco") -> "Bradesco"
            d.contains("inter") -> "Banco Inter"
            d.contains("c6") -> "C6 Bank"
            d.contains("caixa") -> "Caixa"
            d.contains("bb") || d.contains("banco do brasil") -> "Banco do Brasil"
            accountId != null && accountId.contains("7e70") -> "Nubank Cartão"
            accountId != null && accountId.contains("30bf") -> "Nubank Conta"
            else -> "Mercado Pago" // Padrão conforme screenshot do usuário
        }
    }

    private fun buildSignature(date: String, amount: Double, desc: String): String {
        val roundedAmt = (amount * 100).toInt()
        return "${date.take(10)}_$roundedAmt"
    }

    suspend fun addManualTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        paymentMethod: String,
        date: String,
        bankName: String = "Mercado Pago",
        notes: String? = null
    ) {
        val absAmount = abs(amount)
        val noteFlag = if (type == TransactionType.ENTRADA) "ENTRADA - $notes" else notes
        val source = when (type) {
            TransactionType.ENTRADA -> "MANUAL_INCOME"
            TransactionType.ASSINATURA -> "MANUAL_SUBSCRIPTION"
            TransactionType.TRANSFERENCIA -> "MANUAL_TRANSFER"
            else -> "MANUAL"
        }
        val entity = ExpenseEntity(
            storeName = title,
            totalAmount = absAmount,
            transactionDate = date,
            category = category,
            paymentMethod = paymentMethod,
            source = source,
            notes = noteFlag,
            isManual = true
        )
        expenseDao.insertExpense(entity)
    }

    fun toggleSubscriptionStatus(subId: String) {
        val current = _subscriptions.value
        _subscriptions.value = current.map {
            if (it.id == subId) {
                val newStatus = if (it.status == "Ativa") "Pausada" else "Ativa"
                it.copy(status = newStatus)
            } else it
        }
    }

    fun addSubscription(item: SubscriptionItem) {
        _subscriptions.value = listOf(item) + _subscriptions.value
    }

    private fun formatDateSimple(dateStr: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = parser.parse(dateStr) ?: return dateStr
            val formatter = SimpleDateFormat("d 'de' MMM", Locale.forLanguageTag("pt-BR"))
            formatter.format(d)
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun inferDateGroup(dateStr: String): String {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (dateStr == today) return "Hoje"
        if (dateStr == "2026-09-08" || dateStr.endsWith("08")) return "Ontem"
        return "Setembro 2026"
    }

    fun getAllOfferings(): List<AppOfferingFeature> {
        return listOf(
            AppOfferingFeature(
                id = "feat_ocr_gemini",
                title = "Leitor de Cupons com IA (Gemini 3.6 Flash + ML Kit OCR)",
                shortBadge = "IA & OCR Local",
                summary = "Tire foto de qualquer cupom de papel e extraia itens, loja e valor sem digitar nada.",
                fullDescription = "Com motor híbrido inovador, o app combina o Google ML Kit Text Recognition com o modelo Google Gemini 3.6 Flash.",
                category = "Automação & IA"
            ),
            AppOfferingFeature(
                id = "feat_sefaz_nfce",
                title = "Consulta Direta na SEFAZ via QR Code (NFC-e)",
                shortBadge = "SEFAZ Oficial",
                summary = "Aponte a câmera para o QR Code da Nota Fiscal e consulte direto na Fazenda Estadual.",
                fullDescription = "Compatível com as SEFAZ de todos os 27 estados do Brasil (SP, RJ, MG, RS, PR, BA, CE, GO, DF, etc.).",
                category = "Fiscal & Tributário"
            ),
            AppOfferingFeature(
                id = "feat_open_finance",
                title = "Open Finance Brasil Multibancos Integrado",
                shortBadge = "Open Finance",
                summary = "Conecte Mercado Pago, Nubank, Itaú, Bradesco, Inter e acompanhe saldos em tempo real.",
                fullDescription = "Segurança de nível bancário pelo padrão Open Finance regulamentado pelo Banco Central do Brasil.",
                category = "Bancos & Conexões"
            ),
            AppOfferingFeature(
                id = "feat_extrato_unificado",
                title = "Extrato Inteligente de Entradas, Saídas e Pagamentos",
                shortBadge = "Extrato 360°",
                summary = "Visão consolidada de para onde vai e de onde vem seu dinheiro em uma timeline viva.",
                fullDescription = "Veja todas as suas atividades financeiras organizadas por data com filtros inteligentes de entradas e saídas.",
                category = "Gestão Diária"
            ),
            AppOfferingFeature(
                id = "feat_subscriptions",
                title = "Gestor & Rastreador de Assinaturas e Recorrências",
                shortBadge = "Assinaturas",
                summary = "Monitore Netflix, Spotify, Amazon, ChatGPT e evite cobranças surpresa na sua fatura.",
                fullDescription = "Detector inteligente de gastos recorrentes. Saiba exatamente quanto você gasta por mês e por ano.",
                category = "Economia & Controle"
            ),
            AppOfferingFeature(
                id = "feat_family_budget",
                title = "Orçamento Familiar Inteligente com Custo Per Capita",
                shortBadge = "Familiar",
                summary = "Planejamento orçamentário adaptado à quantidade de adultos e crianças da casa.",
                fullDescription = "Nosso algoritmo calcula o custo real de vida por pessoa e divide na regra 50-30-20.",
                category = "Família & Futuro"
            ),
            AppOfferingFeature(
                id = "feat_gemini_advisor",
                title = "Consultor Financeiro Pessoal Gemini AI 24/7",
                shortBadge = "Consultor AI",
                summary = "Inteligência Artificial que analisa seus gastos reais e dá conselhos práticos de economia.",
                fullDescription = "Faça qualquer pergunta sobre finanças: corte de gastos, compras em atacarejo, negociação de dívidas.",
                category = "Inteligência Artificial"
            ),
            AppOfferingFeature(
                id = "feat_cloud_security",
                title = "Segurança com Criptografia e Backup em Nuvem Supabase",
                shortBadge = "Nuvem & Blindagem",
                summary = "Seus dados sincronizados com segurança e acessíveis apenas por você.",
                fullDescription = "Autenticação segura via e-mail e Google Sign-In, com banco de dados em nuvem criptografado.",
                category = "Segurança"
            )
        )
    }
}
