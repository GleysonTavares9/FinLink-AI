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

CREATE TABLE IF NOT EXISTS public.open_finance_accounts (
    id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    bank_name TEXT NOT NULL,
    bank_code TEXT NOT NULL,
    account_type TEXT NOT NULL,
    account_number TEXT NOT NULL,
    current_balance NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    is_connected BOOLEAN NOT NULL DEFAULT true,
    connected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.expenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.household_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.open_finance_accounts ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Usuários gerenciam exclusivamente suas próprias despesas" ON public.expenses;
CREATE POLICY "Usuários gerenciam exclusivamente suas próprias despesas"
    ON public.expenses
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Usuários gerenciam exclusivamente sua composição familiar" ON public.household_profiles;
CREATE POLICY "Usuários gerenciam exclusivamente sua composição familiar"
    ON public.household_profiles
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Usuários gerenciam exclusivamente suas contas bancárias" ON public.open_finance_accounts;
CREATE POLICY "Usuários gerenciam exclusivamente suas contas bancárias"
    ON public.open_finance_accounts
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_expenses_user_id ON public.expenses(user_id);
CREATE INDEX IF NOT EXISTS idx_expenses_date ON public.expenses(transaction_date);
CREATE INDEX IF NOT EXISTS idx_household_user_id ON public.household_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_open_finance_user_id ON public.open_finance_accounts(user_id);
