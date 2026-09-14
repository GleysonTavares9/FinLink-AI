-- ==============================================================================
-- FINLINK AI - SCHEMA COMPLETO DO SUPABASE COM OPEN FINANCE & GOOGLE AUTH
-- ==============================================================================
-- Execute este script no SQL Editor do seu Painel do Supabase:
-- https://supabase.com/dashboard/project/fcwcjnpiavcaaauigulw/sql
-- ==============================================================================

-- 1. TABELA DE DESPESAS (EXPENSES)
CREATE TABLE IF NOT EXISTS public.expenses (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    client_local_id BIGINT,
    store_name TEXT NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    category TEXT NOT NULL DEFAULT 'Alimentação',
    payment_method TEXT NOT NULL DEFAULT 'PIX',
    transaction_date DATE NOT NULL DEFAULT CURRENT_DATE,
    raw_ocr_text TEXT,
    receipt_image_path TEXT,
    is_manual BOOLEAN NOT NULL DEFAULT true,
    source TEXT NOT NULL DEFAULT 'MANUAL',
    notes TEXT,
    items_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_user_client_expense UNIQUE (user_id, client_local_id)
);

-- 2. TABELA DE PERFIL FAMILIAR (HOUSEHOLD PROFILES)
CREATE TABLE IF NOT EXISTS public.household_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    adult_count INT NOT NULL DEFAULT 2,
    child_count INT NOT NULL DEFAULT 1,
    monthly_income NUMERIC(12, 2) NOT NULL DEFAULT 5500.00,
    notes TEXT DEFAULT 'Família padrão',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. TABELA DE CONTAS OPEN FINANCE (PLUGGY / BANCOS)
CREATE TABLE IF NOT EXISTS public.open_finance_accounts (
    id TEXT PRIMARY KEY, -- ID da conta na Pluggy (UUID)
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    item_id TEXT,        -- ID da conexão bancária na Pluggy
    bank_name TEXT NOT NULL,
    bank_code TEXT DEFAULT '000',
    account_type TEXT NOT NULL, -- CHECKING_ACCOUNT, CREDIT_CARD, SAVINGS_ACCOUNT
    account_number TEXT NOT NULL,
    current_balance NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    credit_limit NUMERIC(12, 2) DEFAULT 0.00,
    available_credit_limit NUMERIC(12, 2) DEFAULT 0.00,
    balance_close_date DATE,            -- Data de fechamento da fatura em aberto
    balance_due_date DATE,              -- Data de vencimento da fatura a vencer
    minimum_payment NUMERIC(12, 2),     -- Pagamento mínimo da fatura atual
    credit_card_brand TEXT,             -- MASTERCARD, VISA, ELO, AMEX
    credit_card_level TEXT,             -- PLATINUM, BLACK, GOLD, STANDARD
    credit_card_status TEXT,            -- ACTIVE, BLOCKED
    connector_image_url TEXT,
    is_connected BOOLEAN NOT NULL DEFAULT true,
    connected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. TABELA DE FATURAS DE CARTÃO DE CRÉDITO (BILLS)
CREATE TABLE IF NOT EXISTS public.open_finance_bills (
    id TEXT PRIMARY KEY, -- ID da fatura na Pluggy (UUID)
    account_id TEXT NOT NULL REFERENCES public.open_finance_accounts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    due_date DATE NOT NULL,
    bill_closing_date DATE,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    minimum_payment_amount NUMERIC(12, 2) DEFAULT 0.00,
    allows_installments BOOLEAN DEFAULT true,
    finance_charges JSONB DEFAULT '[]'::jsonb, -- IOF, juros, multas
    payments JSONB DEFAULT '[]'::jsonb,        -- Pagamentos feitos contra a fatura
    is_paid BOOLEAN NOT NULL DEFAULT false,    -- Calculado pela conciliação de pagamentos
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 5. TABELA DE TRANSAÇÕES E EXTRATOS OPEN FINANCE
CREATE TABLE IF NOT EXISTS public.open_finance_transactions (
    id TEXT PRIMARY KEY, -- ID da transação na Pluggy (UUID)
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    account_id TEXT NOT NULL REFERENCES public.open_finance_accounts(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    transaction_date DATE NOT NULL DEFAULT CURRENT_DATE,
    category TEXT DEFAULT 'Outros',
    payment_type TEXT,                      -- CREDIT (entrada) ou DEBIT (saída)
    status TEXT NOT NULL DEFAULT 'POSTED',   -- PENDING (fatura aberta / a vencer) ou POSTED (fechada/paga)
    bill_id TEXT,                           -- ID da fatura correspondente (quando fechada)
    bill_forecast_date TEXT,                -- Previsão da fatura (YYYY-MM) para parcelas futuras
    installment_number INT,                 -- Número da parcela (ex: 1)
    total_installments INT,                 -- Total de parcelas (ex: 6)
    total_installment_amount NUMERIC(12, 2),-- Valor total da compra original parcelada
    operation_type TEXT,                    -- TED, DOC, PIX, CARTAO, BOLETO, etc.
    payment_data JSONB,                     -- Payer, receiver, boletoMetadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ==============================================================================
-- 6. SEGURANÇA: ROW LEVEL SECURITY (RLS)
-- ==============================================================================
ALTER TABLE public.expenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.household_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.open_finance_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.open_finance_bills ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.open_finance_transactions ENABLE ROW LEVEL SECURITY;

-- Políticas para Expenses
DROP POLICY IF EXISTS "expenses_user_policy" ON public.expenses;
CREATE POLICY "expenses_user_policy" ON public.expenses
    FOR ALL TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Políticas para Household Profiles
DROP POLICY IF EXISTS "household_user_policy" ON public.household_profiles;
CREATE POLICY "household_user_policy" ON public.household_profiles
    FOR ALL TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Políticas para Open Finance Accounts
DROP POLICY IF EXISTS "open_finance_accounts_user_policy" ON public.open_finance_accounts;
CREATE POLICY "open_finance_accounts_user_policy" ON public.open_finance_accounts
    FOR ALL TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Políticas para Open Finance Bills
DROP POLICY IF EXISTS "open_finance_bills_user_policy" ON public.open_finance_bills;
CREATE POLICY "open_finance_bills_user_policy" ON public.open_finance_bills
    FOR ALL TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Políticas para Open Finance Transactions
DROP POLICY IF EXISTS "open_finance_transactions_user_policy" ON public.open_finance_transactions;
CREATE POLICY "open_finance_transactions_user_policy" ON public.open_finance_transactions
    FOR ALL TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ==============================================================================
-- 7. ÍNDICES DE PERFORMANCE
-- ==============================================================================
CREATE INDEX IF NOT EXISTS idx_of_transactions_status ON public.open_finance_transactions(status);
CREATE INDEX IF NOT EXISTS idx_of_transactions_forecast ON public.open_finance_transactions(bill_forecast_date);
CREATE INDEX IF NOT EXISTS idx_of_transactions_bill_id ON public.open_finance_transactions(bill_id);
CREATE INDEX IF NOT EXISTS idx_of_bills_account ON public.open_finance_bills(account_id);
-- ==============================================================================
CREATE INDEX IF NOT EXISTS idx_expenses_user_id ON public.expenses(user_id);
CREATE INDEX IF NOT EXISTS idx_expenses_date ON public.expenses(transaction_date);
CREATE INDEX IF NOT EXISTS idx_household_user_id ON public.household_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_open_finance_user_id ON public.open_finance_accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_open_finance_tx_account ON public.open_finance_transactions(account_id);
CREATE INDEX IF NOT EXISTS idx_open_finance_tx_user ON public.open_finance_transactions(user_id);

-- ==============================================================================
-- 7. TRIGGER AUTOMÁTICO PARA CRIAÇÃO DE PERFIL AO LOGAR (GOOGLE OU E-MAIL)
-- ==============================================================================
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.household_profiles (user_id, adult_count, child_count, monthly_income, notes)
    VALUES (NEW.id, 1, 0, 3000.00, 'Perfil inicial do usuário')
    ON CONFLICT (user_id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();


-- ==============================================================================
-- 8. SISTEMA MULTI-AGENTE DE IA FINANCEIRA (Albert, Sherlock, Galileu & Custom)
-- ==============================================================================

-- Tabela de Limites de Orcamento para o Agente Sherlock (80% de Alerta)
CREATE TABLE IF NOT EXISTS public.budget_limits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    category TEXT NOT NULL,
    monthly_limit NUMERIC(12, 2) NOT NULL DEFAULT 500.00,
    alert_threshold_percentage NUMERIC(5, 2) NOT NULL DEFAULT 80.00,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()),
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()),
    CONSTRAINT unique_user_category UNIQUE (user_id, category)
);

-- Tabela de Agentes Personalizados No-Code (Gatilho -> Condicao -> Acao)
CREATE TABLE IF NOT EXISTS public.custom_agents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT,
    natural_prompt TEXT NOT NULL,
    rule_json JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_triggered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()),
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW())
);

-- Tabela de Alertas e Notificacoes dos Agentes (Albert, Sherlock, Galileu, Custom)
CREATE TABLE IF NOT EXISTS public.agent_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    agent_name TEXT NOT NULL, -- 'albert', 'sherlock', 'galileu', ou nome custom
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    severity TEXT NOT NULL DEFAULT 'info', -- 'info', 'warning', 'critical'
    metadata JSONB,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW())
);

-- Tabela de Relatorios Estrategicos Mensais do Agente Galileu
CREATE TABLE IF NOT EXISTS public.monthly_strategic_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    month_year TEXT NOT NULL, -- ex: '2026-09'
    total_income NUMERIC(12, 2) DEFAULT 0.00,
    total_expenses NUMERIC(12, 2) DEFAULT 0.00,
    savings_rate NUMERIC(5, 2) DEFAULT 0.00,
    report_markdown TEXT NOT NULL,
    key_takeaways JSONB,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()),
    CONSTRAINT unique_user_month_report UNIQUE (user_id, month_year)
);

-- Habilitar RLS em todas as novas tabelas
ALTER TABLE public.budget_limits ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.custom_agents ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.agent_notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.monthly_strategic_reports ENABLE ROW LEVEL SECURITY;

-- Politicas RLS
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'Users can manage their own budget limits') THEN
        CREATE POLICY "Users can manage their own budget limits"
            ON public.budget_limits FOR ALL TO authenticated
            USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'Users can manage their own custom agents') THEN
        CREATE POLICY "Users can manage their own custom agents"
            ON public.custom_agents FOR ALL TO authenticated
            USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'Users can view and update their agent notifications') THEN
        CREATE POLICY "Users can view and update their agent notifications"
            ON public.agent_notifications FOR ALL TO authenticated
            USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'Users can view their monthly strategic reports') THEN
        CREATE POLICY "Users can view their monthly strategic reports"
            ON public.monthly_strategic_reports FOR ALL TO authenticated
            USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
    END IF;
END $$;

-- Indices para Performance dos Agentes
CREATE INDEX IF NOT EXISTS idx_budget_limits_user ON public.budget_limits(user_id);
CREATE INDEX IF NOT EXISTS idx_custom_agents_user ON public.custom_agents(user_id, is_active);
CREATE INDEX IF NOT EXISTS idx_agent_notif_user ON public.agent_notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_monthly_reports_user ON public.monthly_strategic_reports(user_id, month_year);
