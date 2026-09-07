# FinLink AI (Cupom & Finanças AI) — Controlador de Gastos com IA

Aplicativo Android nativo em **Kotlin + Jetpack Compose** que realiza a leitura de cupons fiscais (foto com **Gemini Vision** ou dados oficiais de **QR Code / NFC-e na SEFAZ**), organiza receitas e despesas, integra contas via **Open Finance**, disponibiliza um **Painel Web** embutido e utiliza o **Google Gemini** como consultor e educador financeiro que aprende com seu histórico e recomenda orçamentos adaptados à composição familiar da casa (número de adultos e crianças).

---

## 1. Visão Geral da Arquitetura

O aplicativo segue os princípios modernos de arquitetura Android com separação clara de responsabilidades:

```
UI (Jetpack Compose)
   │
   ▼
ViewModel (ExpenseViewModel)  ───────────►  UseCases (Domain)
   │                                              │  ├── CalculateBudgetUseCase
   ▼                                              ▼  └── (Regras de Negócio)
Repository (Expense / OpenFinance) ◄──────►  AiProvider (Interface Plugável)
   │                                              │  ├── GeminiProvider (gemini-2.0-flash / 3.5)
   ▼                                              └── LocalFallback (Offline resiliente)
Room DB (SQLite Offline-First)
   ├── ExpenseDao (Despesas & Cupons)
   ├── OpenFinanceDao (Contas bancárias & Extratos)
   └── HouseholdDao (Perfil e Composição Familiar)
```

### Por que arquitetura de IA plugável?
A interface `AiProvider` (`com.example.data.remote.ai.AiProvider`) isola qualquer dependência de modelos de Inteligência Artificial:
* **`GeminiProvider`**: implementa a extração estruturada de cupons e orientações financeiras usando a API do Google Gemini.
* **`AiProviderFactory`**: permite alternar dinamicamente entre versões do Gemini ou fallback offline local sem quebrar o restante da aplicação.

---

## 2. Métodos de Leitura do Cupom Fiscal

O app disponibiliza os dois métodos complementares para captura de gastos:

1. **QR Code / URL NFC-e (SEFAZ)** — *Método Preferencial e sem custo de IA*:
   * Lê a URL do QR Code da NFC-e ou a chave de acesso de 44 dígitos.
   * `NfceFetcher`: realiza a requisição HTTP aos portais estaduais da SEFAZ com tratamento resiliente de conexão.
   * `NfceParser`: extrai razão social do estabelecimento, CNPJ, UF, data de emissão, itens detalhados (quantidade, valor unitário e total), valor final e forma de pagamento.
2. **Foto + Gemini Vision (Multimodal)** — *Para cupons térmicos, recibos manuais e feiras*:
   * O usuário fotografa o cupom ou seleciona uma imagem da galeria.
   * A imagem é codificada em Base64 e enviada ao modelo Gemini pedindo estritamente saída JSON padronizada.
   * Conta com fallback inteligente local para garantir funcionamento mesmo em ambientes sem conectividade.

Ambos os fluxos convertem o resultado para `ScannedReceiptResult`, permitindo que o usuário revise, edite valores/categorias e confirme antes de persistir no banco Room.

---

## 3. Orçamento Doméstico por Composição Familiar (Adultos & Crianças)

Diferente de gerenciadores genéricos, o FinLink AI calcula o orçamento levando em conta **quem reside na casa**:

* **Tabela de Referência (`HouseholdCostReference`)**:
  * Define custos básicos médios por adulto e por criança para Alimentação/Mercado, Moradia, Saúde, Transporte, Lazer e Outros (com base em dados demográficos como POF/IBGE e DIEESE).
* **Caso de Uso (`CalculateBudgetUseCase`)**:
  * Compara o histórico de despesas com o teto ideal para a composição cadastrada (ex.: 2 adultos + 1 criança).
  * Calcula o gasto **per capita** por pessoa da residência.
  * Identifica desvios por categoria (ex.: *Alimentação 25% acima do esperado para 3 pessoas*).
  * Projeta o valor de fechamento do mês com base no ritmo diário de despesas.
* **Integração com o Consultor Gemini**:
  * O contexto familiar (número de adultos, dependentes infantis e diagnóstico de categorias) é enviado ao Gemini, gerando conselhos acionáveis e dicas práticas focadas no perfil da casa.

---

## 4. Recursos Integrados do FinLink AI

* **Dashboard Principal**: Cards em estilo Titanium/Obsidian (estética Apple Wallet), balanço mensal, progresso do orçamento familiar, métricas por categoria e filtros dinâmicos de busca.
* **Open Finance**: Conexão simulada de bancos (Nubank, Itaú, Bradesco, Inter), importação manual de extratos e exibição da API Specification em JSON.
* **Painel Web Local**: Servidor/dashboard web integrado gerando relatórios responsivos HTML/CSS/JS exportáveis.
* **Lançamento Manual Rápido**: Para pequenos pagamentos diários com categorização e notas.
* **Nuvem & Segurança Supabase**: Cadastro, login, gerenciamento de sessão com JWT e sincronização criptografada.

---

## 5. Segurança Máxima com Supabase (Auth + RLS)

O FinLink AI foi arquitetado para proteger rigorosamente a privacidade financeira do usuário:

1. **Autenticação Segura (GoTrue Auth)**:
   * Cadastro e login com Email/Senha emitindo tokens JWT (`access_token` e `refresh_token`).
   * Persistência segura de sessão local no Android via `SupabaseSessionManager`.
2. **Row Level Security (RLS) no PostgreSQL**:
   * O script [supabase_schema.sql](file:///c:/Users/GleysonMiranda/Downloads/FinLink%20AI/supabase_schema.sql) habilita políticas estritas de isolamento:
     ```sql
     CREATE POLICY "Usuários gerenciam exclusivamente suas próprias despesas"
         ON public.expenses FOR ALL TO authenticated
         USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
     ```
   * Mesmo que a chave pública anônima seja conhecida, é **matematicamente impossível** que qualquer usuário consulte ou manipule registros de outros.
3. **Sincronização Criptografada (HTTPS/TLS)**:
   * Sincronização bidirecional entre o Room DB offline e o Supabase via PostgREST.

---

## 6. Estrutura de Pastas do Projeto

```
app/src/main/java/com/example/
├── data/
│   ├── db/                     # Room Database, DAOs e Entidades
│   │   ├── AppDatabase.kt
│   │   ├── ExpenseDao.kt & ExpenseEntity.kt
│   │   ├── OpenFinanceDao.kt & OpenFinanceEntity.kt
│   │   └── HouseholdDao.kt & HouseholdEntity.kt
│   ├── gemini/                 # Tipos e estruturas de resultado de cupom
│   │   └── GeminiRepository.kt
│   ├── openfinance/            # Repositório de Open Finance e bancos
│   │   └── OpenFinanceRepository.kt
│   └── remote/
│       ├── ai/                 # AiProvider, GeminiProvider e AiProviderFactory
│       ├── nfce/               # NfceFetcher e NfceParser (SEFAZ)
│       └── supabase/           # SupabaseClient, SupabaseAuthRepository, SupabaseSyncRepository
├── domain/
│   ├── model/                  # HouseholdProfile, HouseholdCostReference, BudgetAnalysis
│   └── usecase/                # CalculateBudgetUseCase
├── ui/
│   ├── screens/                # Telas em Jetpack Compose
│   │   ├── DashboardScreen.kt
│   │   ├── ScanReceiptScreen.kt
│   │   ├── HouseholdScreen.kt
│   │   ├── AuthScreen.kt       # Login, Cadastro e Sincronização Supabase
│   │   ├── ManualInputScreen.kt
│   │   ├── OpenFinanceScreen.kt
│   │   ├── WebDashboardScreen.kt
│   │   └── GeminiAssistantScreen.kt
│   ├── theme/                  # Tipografia, paleta iOS/Emerald e temas
│   └── viewmodel/              # ExpenseViewModel e estados de UI
├── web/                        # Template do dashboard web
└── MainActivity.kt             # Entrada da aplicação e configuração de navegação (NavHost)
```

---

## 7. Como Rodar o Projeto

1. Abra a pasta `FinLink AI` no **Android Studio** (Hedgehog ou superior).
2. Configure as chaves no arquivo `.env` na raiz do projeto (ou copie de `.env.example`):
   ```properties
   # Gemini API Key (IA Multimodal)
   GEMINI_API_KEY=sua_chave_gemini_aqui

   # Supabase (Banco na Nuvem + Auth + RLS)
   SUPABASE_URL=https://seu-projeto.supabase.co
   SUPABASE_ANON_KEY=sua_chave_anonima_supabase_aqui
   ```
   * *Nota: caso não configure as chaves de imediato, o aplicativo continuará executando normalmente em modo de demonstração local offline.*
3. (Opcional) No painel do seu projeto Supabase, acesse **SQL Editor**, cole e execute o script [supabase_schema.sql](file:///c:/Users/GleysonMiranda/Downloads/FinLink%20AI/supabase_schema.sql) para criar as tabelas com as políticas de RLS ativadas.
4. Execute o **Gradle Sync** no Android Studio.
5. Execute no seu dispositivo físico ou emulador (Min SDK: 24 / Android 7.0+).
