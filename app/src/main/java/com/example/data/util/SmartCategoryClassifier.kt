package com.example.data.util

import java.util.Locale

/**
 * Classificador e agrupador orçamentário inteligente do FinLink AI.
 * Identifica o grupo orçamentário e a categoria de forma automática a partir
 * do nome do estabelecimento, descrição da conta ou itens do cupom fiscal.
 *
 * Exemplos de auto-agrupamento automático:
 * - Água, Luz, Energia, Internet, Condomínio, Aluguel, Gás -> Moradia
 * - Supermercado, Atacarejo, EPA, Mart Minas, Hortifruti -> Mercado
 * - Restaurante, iFood, Lanchonete, Hamburgueria -> Alimentação
 * - Farmácia, Drogaria, Medicamento, Hospital -> Saúde
 * - Uber, 99, Posto, Gasolina, Combustível, Pedágio -> Transporte
 * - Netflix, Cinema, Spotify, Jogos, Streaming -> Lazer
 * - Shopee, Mercado Livre, Amazon, Vestuário -> Compras
 * - Salário, Pro-labore, Rendimentos, Dividendos -> Salário
 */
object SmartCategoryClassifier {

    fun classifyCategory(text: String, items: List<String> = emptyList()): String {
        val combined = (text + " " + items.joinToString(" ")).lowercase(Locale.ROOT)

        return when {
            // 1. MORADIA / CONTAS BÁSICAS RESIDENCIAIS (Prioridade solicitada pelo usuário)
            combined.contains("agua") || combined.contains("água") ||
            combined.contains("copasa") || combined.contains("sanepar") || combined.contains("sabesp") ||
            combined.contains("embasa") || combined.contains("compesa") || combined.contains("caesb") ||
            combined.contains("luz") || combined.contains("energia") || combined.contains("eletric") ||
            combined.contains("cemig") || combined.contains("enel") || combined.contains("cpfl") ||
            combined.contains("eletrobras") || combined.contains("energisa") || combined.contains("light") ||
            combined.contains("internet") || combined.contains("banda larga") || combined.contains("fibra") ||
            combined.contains("wifi") || combined.contains("wi-fi") || combined.contains("telecom") ||
            combined.contains("claro") || combined.contains("vivo") || combined.contains("tim") ||
            combined.contains("oi fibra") || combined.contains("aluguel") || combined.contains("condominio") ||
            combined.contains("condomínio") || combined.contains("iptu") || combined.contains("taxa de lixo") ||
            combined.contains("gas") || combined.contains("gás") || combined.contains("botijao") ||
            combined.contains("botijão") || combined.contains("ultragaz") || combined.contains("supergasbras") ||
            combined.contains("nacional gas") || combined.contains("liquigas") || combined.contains("manutencao casa") ||
            combined.contains("reforma") || combined.contains("material de construcao") -> "Moradia"

            // 2. MERCADO E SUPERMERCADOS
            combined.contains("supermercado") || combined.contains("mercado") || combined.contains("epa") ||
            combined.contains("mart minas") || combined.contains("carrefour") || combined.contains("assai") ||
            combined.contains("assaí") || combined.contains("atacadao") || combined.contains("atacadão") ||
            combined.contains("bh supermercados") || combined.contains("sacolao") || combined.contains("sacolão") ||
            combined.contains("hortifruti") || combined.contains("acougue") || combined.contains("açougue") ||
            combined.contains("padaria") || combined.contains("pao de minas") || combined.contains("pao mais") ||
            combined.contains("pão") || combined.contains("mercearia") -> "Mercado"

            // 3. ALIMENTAÇÃO E REFEIÇÕES
            combined.contains("restaurante") || combined.contains("lanchonete") || combined.contains("lanche") ||
            combined.contains("ifood") || combined.contains("burger") || combined.contains("hamburguer") ||
            combined.contains("pizza") || combined.contains("pizzaria") || combined.contains("mc donalds") ||
            combined.contains("mcdonalds") || combined.contains("subway") || combined.contains("bar") ||
            combined.contains("cafe") || combined.contains("café") || combined.contains("almoco") ||
            combined.contains("almoço") || combined.contains("churrascaria") || combined.contains("marmita") -> "Alimentação"

            // 4. SAÚDE E MEDICAMENTOS
            combined.contains("farmacia") || combined.contains("farmácia") || combined.contains("drogaria") ||
            combined.contains("lucena") || combined.contains("drogasil") || combined.contains("pague menos") ||
            combined.contains("droga raia") || combined.contains("araujo") || combined.contains("remedio") ||
            combined.contains("remédio") || combined.contains("medicamento") || combined.contains("hospital") ||
            combined.contains("clinica") || combined.contains("clínica") || combined.contains("consulta") ||
            combined.contains("dentista") || combined.contains("odont") || combined.contains("laboratorio") ||
            combined.contains("exame") || combined.contains("otica") || combined.contains("ótica") -> "Saúde"

            // 5. TRANSPORTE E COMBUSTÍVEL
            combined.contains("posto") || combined.contains("gasolina") || combined.contains("etanol") ||
            combined.contains("combustivel") || combined.contains("combustível") || combined.contains("diesel") ||
            combined.contains("ipiranga") || combined.contains("shell") || combined.contains("petrobras") ||
            combined.contains("br mania") || combined.contains("uber") || combined.contains("99") ||
            combined.contains("taxi") || combined.contains("táxi") || combined.contains("pedagio") ||
            combined.contains("pedágio") || combined.contains("estacionamento") || combined.contains("sem parar") ||
            combined.contains("veloe") || combined.contains("onibus") || combined.contains("ônibus") ||
            combined.contains("metro") || combined.contains("metrô") || combined.contains("oficina") ||
            combined.contains("pneu") || combined.contains("auto pecas") || combined.contains("lavajato") -> "Transporte"

            // 6. LAZER, STREAMING E ENTRETENIMENTO
            combined.contains("netflix") || combined.contains("spotify") || combined.contains("prime video") ||
            combined.contains("amazon prime") || combined.contains("disney") || combined.contains("hbo") ||
            combined.contains("max") || combined.contains("youtube") || combined.contains("cinema") ||
            combined.contains("cine") || combined.contains("ingresso") || combined.contains("show") ||
            combined.contains("teatro") || combined.contains("steam") || combined.contains("playstation") ||
            combined.contains("xbox") || combined.contains("nintendo") || combined.contains("jogos") ||
            combined.contains("clube") || combined.contains("viagem") || combined.contains("hotel") ||
            combined.contains("pousada") -> "Lazer"

            // 7. COMPRAS E VESTUÁRIO
            combined.contains("shopee") || combined.contains("mercado livre") || combined.contains("mercadolivre") ||
            combined.contains("amazon") || combined.contains("shein") || combined.contains("aliexpress") ||
            combined.contains("magalu") || combined.contains("magazine luiza") || combined.contains("casas bahia") ||
            combined.contains("marisa") || combined.contains("renner") || combined.contains("c&a") ||
            combined.contains("riachuelo") || combined.contains("zara") || combined.contains("roupa") ||
            combined.contains("calcado") || combined.contains("calçado") || combined.contains("tenis") ||
            combined.contains("tênis") || combined.contains("vestuario") || combined.contains("vestuário") -> "Compras"

            // 8. SALÁRIO E RECEITAS
            combined.contains("salario") || combined.contains("salário") || combined.contains("pro-labore") ||
            combined.contains("pró-labore") || combined.contains("rendimento") || combined.contains("dividendo") ||
            combined.contains("comissao") || combined.contains("comissão") || combined.contains("freelance") ||
            combined.contains("bico") || combined.contains("restituicao") || combined.contains("restituição") -> "Salário"

            else -> "Outros"
        }
    }
}