# Pluggy API — Guia Completo: Cartão de Crédito, Faturas, Parcelas e Transações

> Referência técnica para exploração máxima da API Pluggy nos temas: fatura em aberto, parcelas futuras, entradas/saídas (créditos/débitos) e valores a vencer.
> Fontes: docs.pluggy.ai (Accounts, Transactions, Credit Card Bills, Credit Card Installments, Open Finance Coverage).

---

## 1. Visão geral do modelo de dados

Para "explorar ao máximo" a API em cartão de crédito você tem **3 entidades** e **1 relação temporal** para combinar:

| Entidade | Endpoint | O que traz |
|---|---|---|
| `Account` (type=CREDIT) | `GET /accounts?itemId=` | Limite, saldo atual, datas de fechamento/vencimento, bandeira, pagamento mínimo |
| `Bill` | `GET /bills?accountId=` | Fatura fechada/vencida: valor total, mínimo, encargos, pagamentos |
| `Transaction` | `GET /v2/transactions?accountId=` | Cada lançamento (compra, parcela, pagamento, encargo) |
| Webhooks | `transactions/created`, `transactions/updated`, `transactions/deleted` | Notificam mudanças assíncronas (essencial para não perder parcelas) |

O pulo do gato: **fatura em aberto (open) não existe como objeto `Bill`** até fechar/vencer. Enquanto isso, tudo o que "está por vir" mora nas `Transactions` com `status: PENDING` e sem `billId`.

---

## 2. Account (Cartão de Crédito) — `type: CREDIT`

```json
{
  "id": "4f61bd6d-e6fc-44b2-9c4b-5609058de7ab",
  "type": "CREDIT",
  "subtype": "CREDIT_CARD",
  "name": "Itau Uniclass 2.0 Mastercard Platinum",
  "balance": 142.41,
  "currencyCode": "BRL",
  "creditData": {
    "level": "PLATINUM",
    "brand": "MASTERCARD",
    "balanceCloseDate": "2020-07-08",
    "balanceDueDate": "2020-07-17",
    "availableCreditLimit": 51300,
    "creditLimit": 51800,
    "isLimitFlexible": false,
    "balanceForeignCurrency": 500,
    "minimumPayment": 100,
    "status": "ACTIVE",
    "holderType": "MAIN"
  }
}
```

### Campos-chave para "fatura aberta / a vencer"

| Campo | Uso prático |
|---|---|
| `balance` | Valor da fatura **em aberto** ainda não paga (a "aberta"). Para Open Finance, representa o limite já usado. |
| `creditData.balanceCloseDate` | Data em que essa fatura fecha |
| `creditData.balanceDueDate` | **Data de vencimento (a vencer)** |
| `creditData.minimumPayment` | Pagamento mínimo da fatura atual |
| `creditData.availableCreditLimit` | Limite ainda disponível |
| `creditData.creditLimit` | Limite total |

**Fórmula útil (360° do cartão):**
```
creditLimit = availableCreditLimit (a gastar) + balance (fatura aberta) + dívida de faturas anteriores não quitadas
```

### `disaggregatedCreditLimits` (avançado)
Alguns cartões têm múltiplas linhas de crédito (crédito à vista, parcelado, saque, consignado). Use esse array quando o saldo/limite "não bate" — ele desmembra por `lineName` (`CREDITO_A_VISTA`, `CREDITO_PARCELADO`, `SAQUE_CREDITO_BRASIL`, `SAQUE_CREDITO_EXTERIOR`, `EMPRESTIMO_CARTAO_CONSIGNADO`, `OUTROS`), trazendo `limitAmount`, `usedAmount`, `availableAmount` individualizados.

---

## 3. Bill (Fatura fechada/vencida)

⚠️ **Só existe fatura fechada.** Enquanto a fatura está "em aberto", ela não aparece como `Bill` — você monta a visão de "fatura aberta" somando as `Transactions` pendentes daquele ciclo (ver seção 5).

Disponível obrigatoriamente em conectores **Open Finance Regulado**. Em Direct Connectors, só Inter PF e Itaú Cartões retornam.

```json
{
  "id": "76edd0fa-57b4-4391-be8d-e8b6249d04b1",
  "dueDate": "2023-09-15T00:00:00.000Z",
  "billClosingDate": null,
  "totalAmount": 10000.76,
  "totalAmountCurrencyCode": "BRL",
  "minimumPaymentAmount": 3000,
  "allowsInstallments": true,
  "payments": [
    {
      "id": "a1b2c3d4-...",
      "valueType": "FULL_PAYMENT",
      "paymentDate": "2023-09-15T00:00:00.000Z",
      "paymentMode": "PIX",
      "amount": 10000.76,
      "currencyCode": "BRL"
    }
  ],
  "financeCharges": [
    { "id": "6119c3e0-...", "type": "IOF", "amount": 7.01, "currencyCode": "BRL" }
  ]
}
```

| Campo | Descrição |
|---|---|
| `dueDate` | Data de vencimento (a vencer) |
| `billClosingDate` | Data de fechamento |
| `totalAmount` | **Total da fatura** |
| `minimumPaymentAmount` | Mínimo a pagar |
| `allowsInstallments` | Se permite parcelar a fatura |
| `financeCharges[].type` | `LATE_PAYMENT_REMUNERATIVE_INTEREST`, `LATE_PAYMENT_FEE`, `LATE_PAYMENT_INTEREST`, `IOF`, `OTHER` |
| `payments[]` | Pagamentos feitos contra essa fatura |

### ✅ Como verificar se uma fatura foi quitada (fórmula oficial)
```
totalAmount(Fatura N) + soma(financeCharges da Fatura N+1) = soma(payments da Fatura N+1)
```
Exemplo: Janeiro R$500 + encargos de Fevereiro R$12,50 = pagamentos de Fevereiro R$512,50 → Janeiro **quitada**.

Endpoints: `GET /bills?accountId=` (lista) e `GET /bills/{id}` (detalhe).

---

## 4. Transaction — o coração de "entradas e saídas"

Endpoint recomendado: `GET /v2/transactions?accountId=...` (paginação por cursor, páginas de 500). O endpoint por página (`/transactions`) está **deprecado até 31/12/2026**.

```json
{
  "id": "6ec156fe-e8ac-4d9a-a4b3-7770529ab01c",
  "description": "TED Example",
  "descriptionRaw": null,
  "amount": 1500,
  "date": "2021-04-12T00:00:00.000Z",
  "balance": 3500,
  "currencyCode": "BRL",
  "category": "Transfer",
  "type": "CREDIT",
  "status": "POSTED",
  "operationType": "TED",
  "operationTypeAdditionalInfo": null,
  "paymentData": null,
  "creditCardMetadata": {
    "installmentNumber": 1,
    "totalInstallments": 6,
    "totalAmount": 9000
  },
  "merchant": null,
  "providerId": null
}
```

### Entradas (crédito) vs Saídas (débito)

| Campo | Regra |
|---|---|
| `type` | `DEBIT` (saída) ou `CREDIT` (entrada) |
| `amount` (conta corrente) | Positivo = entrada / Negativo = saída (padrão comum) |
| `amount` (cartão de crédito) | **Invertido**: positivo = compra/débito (aumenta a dívida); negativo = pagamento/crédito (reduz a dívida) |

### Status = "a vencer" / "em aberto"

| Status | Significado |
|---|---|
| `PENDING` | Transação está numa fatura **ainda aberta** (não vencida) ou é parcela futura → **sem `billId`** |
| `POSTED` | Transação já está em fatura **fechada/vencida** → **com `billId`** |

➡️ **Para listar tudo que está "a vencer" numa fatura de cartão**: filtre `accountId` do cartão + `status=PENDING`.

### `operationType` (Open Finance) — classifica a natureza do lançamento
`TED`, `DOC`, `PIX`, `TRANSFERENCIA_MESMA_INSTITUICAO`, `BOLETO`, `CONVENIO_ARRECADACAO`, `PACOTE_TARIFA_SERVICOS`, `TARIFA_SERVICOS_AVULSOS`, `FOLHA_PAGAMENTO`, `DEPOSITO`, `SAQUE`, `CARTAO`, `ENCARGOS_JUROS_CHEQUE_ESPECIAL`, `RENDIMENTO_APLIC_FINANCEIRA`, `PORTABILIDADE_SALARIO`, `RESGATE_APLIC_FINANCEIRA`, `OPERACAO_CREDITO`, `OUTROS`.

> ⚠️ C6 Bank não classifica por `operationType` — tudo vem como `OUTROS`.

### `creditCardMetadata` — o detalhamento de parcelas

```json
{
  "installmentNumber": 1,
  "totalInstallments": 6,
  "totalAmount": 9000,
  "payeeMCC": 1234,
  "cardNumber": "1234",
  "billId": "03cc0eff-...",
  "feeType": "ANNUAL_FEE",
  "feeTypeAdditionalInfo": null,
  "otherCreditsType": "BILL_INSTALLMENT",
  "otherCreditsAdditionalInfo": null,
  "billForecastDate": null
}
```

| Campo | Para que serve |
|---|---|
| `installmentNumber` / `totalInstallments` | Nº da parcela / total de parcelas |
| `totalAmount` | Valor total da compra (soma de todas as parcelas) |
| `purchaseDate` | Data original da compra (para parcelas) |
| `billId` | Fatura à qual pertence — **ausente enquanto a fatura não vence** |
| `billForecastDate` | **Previsão (YYYY-MM) da fatura em que essa transação vai cair** — funciona mesmo para transações pendentes/futuras. **Este é o campo-chave para prever "valores a vencer" mês a mês, antes mesmo de existir `billId`.** (Só Open Finance) |
| `feeType` | Taxas: `ANNUAL_FEE`, `ATM_WITHDRAWAL_DOMESTIC`, `ATM_WITHDRAWAL_INTERNATIONAL`, `EMERGENCY_CREDIT_EVALUATION`, `CARD_REISSUE`, `BILL_PAYMENT_FEE`, `SMS`, `OTHER` |
| `otherCreditsType` | Operações de crédito: `REVOLVING_CREDIT`, `BILL_INSTALLMENT`, `LOAN`, `OTHER` |

### `paymentData` (transferências/pagamentos)
Traz `payer`, `receiver` (nome, CPF/CNPJ, agência, conta, banco), `paymentMethod` (PIX/TED/DOC), `referenceNumber`, `reason`, e `boletoMetadata` (linha digitável, valor base, juros, multa, desconto) quando aplicável.

### `merchant` (Pro)
`name`, `businessName`, `cnpj`, `cnae`, `category` — exige categorização habilitada + plano Pro.

---

## 5. Como montar a "fatura em aberto" e "valores a vencer" (prático)

Como não existe `Bill` para fatura aberta, você **constrói** essa visão assim:

1. Pegue o `Account` (type=CREDIT) → `balance` já te dá o total da fatura aberta e `creditData.balanceDueDate` a data de vencimento.
2. Para o **detalhamento linha a linha** dessa fatura aberta, busque `Transactions` do `accountId` com `status=PENDING`.
3. Use `creditCardMetadata.billForecastDate` para agrupar transações pendentes por mês de fatura futura (ex.: parcelas 2/6, 3/6 que ainda vão vencer em meses seguintes).
4. Quando a fatura fecha/vence, a Pluggy dispara `transactions/updated` — as transações daquele ciclo ganham `billId` e viram `status=POSTED`; **nesse momento também passa a existir o objeto `Bill`** correspondente.

### Padrões de lançamento de parcelas por banco
- **Padrão A**: banco lança todas as parcelas de uma vez, já na compra (1ª com data da compra, demais com `billPostDate` = mês de vencimento).
- **Padrão B** (mais comum): banco cria uma parcela nova a cada fatura fechada/vencida — parcelas futuras "não existem" ainda como transação.
- **Janela fechamento→vencimento**: em alguns bancos, ao fechar a fatura já aparece a parcela da próxima fatura (ainda não vencida).

⚠️ **Não existe ID de agrupamento oficial de parcelamento no Open Finance.** Para juntar parcelas da mesma compra, use heurística: `installmentNumber` + `totalInstallments` + `totalAmount` + nome do estabelecimento.

---

## 6. Webhooks — obrigatórios para não perder dado

| Evento | Quando dispara | Ação recomendada |
|---|---|---|
| `transactions/created` | Nova parcela/transação surgiu fora da janela de 7 dias | Paginar `createdTransactionsLink` e inserir |
| `transactions/updated` | Transação mudou (ex: `PENDING`→`POSTED`, ganhou `billId`) | Buscar por `ids` recebidos e atualizar |
| `transactions/deleted` | Transação foi removida (raro; recriada com novo ID depois) | Deletar localmente pelos `ids` |

> O `id` da transação pode mudar se a instituição alterar data/descrição/valor de forma que a Pluggy não consiga confirmar que é "a mesma" transação — nesse caso ela é deletada e recriada.

**Recomendação de paginação**: page size de 500 (padrão desde 01/12/2025).

---

## 7. Limites operacionais (Open Finance) que afetam sincronização de cartão

| Produto | Limite mensal | Quando é consumido |
|---|---|---|
| Lista de contas/cartões | 4 | Criação do item e a cada 7 dias |
| Faturas e transações de fatura | 30 | Criação e 1x/dia |
| Limites do cartão | 240 | A cada atualização |
| Transações recentes (1–6 dias) | 240 | A cada atualização |
| Transações não-recentes (7–365 dias) | 4 | Criação e a cada 7 dias |

Passou do limite → item fica `PARTIAL_SUCCESS` com warning `code: "423"` no `statusDetail`. **Evite múltiplos items para o mesmo CPF/CNPJ + instituição** — isso consome o limite mais rápido.

---

## 8. Diferenças importantes entre bancos (parcelas/fatura)

- **BTG Pactual (614)**: parcelas futuras (nº ≥ 2) já vêm na 1ª conexão com status `PENDING`.
- **XP Banking (602)**: pode retornar o valor total da compra parcelada somado (nunca terá `billId`).
- **Itaú Empresas (618)**: sempre retorna transações **detalhadas** (não consolidadas) com CPF/CNPJ da contraparte.
- **C6 Bank**: não classifica por `operationType` (tudo `OUTROS`).
- **Contas correntes**: limite de 260 contas por item — acima disso não são coletadas.

---

## 9. Checklist de exploração máxima da API (o que seu app deveria consumir)

- [ ] `GET /accounts?itemId=` (type=CREDIT) → limite, fatura aberta (`balance`), vencimento, fechamento, mínimo
- [ ] `GET /accounts/{id}` → `disaggregatedCreditLimits` se houver múltiplas linhas de crédito
- [ ] `GET /bills?accountId=` → histórico de faturas fechadas, encargos, pagamentos
- [ ] `GET /v2/transactions?accountId=&status=PENDING` → tudo que está **a vencer / em aberto**
- [ ] `GET /v2/transactions?accountId=&status=POSTED` → histórico já fechado/pago
- [ ] Ler `creditCardMetadata.billForecastDate` em cada transação pendente → projeção de fatura por mês futuro
- [ ] Ler `type` + `amount` (lembrando do sinal invertido em cartão) → separar entradas/saídas
- [ ] Configurar webhooks `transactions/created|updated|deleted`
- [ ] Tratar `PARTIAL_SUCCESS` / warning `423` (rate limit Open Finance)
- [ ] Aplicar fórmula de conciliação de fatura quitada (seção 3) para status de pagamento

---

## 10. Referências

- Accounts: https://docs.pluggy.ai/docs/accounts
- Credit Card Bills: https://docs.pluggy.ai/docs/credit-card-bills
- Credit Card Installments (comportamento por banco): https://docs.pluggy.ai/docs/credit-card-installments
- Transactions: https://docs.pluggy.ai/docs/transactions
- Rate Limits Open Finance: https://docs.pluggy.ai/docs/rate-limits-of
- Considerations & FAQ (OF): https://docs.pluggy.ai/docs/considerations-faq
- API Reference (endpoints completos): https://docs.pluggy.ai/reference
