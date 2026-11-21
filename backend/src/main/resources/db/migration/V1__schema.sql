-- PocketLens schema. Every user-owned table references users(id) with ON DELETE CASCADE, so deleting an account
-- removes all personal data in one statement (GDPR Art. 17, US-6.4).

CREATE TABLE users (
    id                     UUID PRIMARY KEY,
    email                  VARCHAR(254) NOT NULL UNIQUE,
    password_hash          VARCHAR(100) NOT NULL,
    display_name           VARCHAR(80)  NOT NULL,
    role                   VARCHAR(16)  NOT NULL,
    privacy_notice_version VARCHAR(16)  NOT NULL,
    consent_given_at       TIMESTAMPTZ  NOT NULL,
    created_at             TIMESTAMPTZ  NOT NULL
);

CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE categories (
    id         UUID PRIMARY KEY,
    user_id    UUID REFERENCES users (id) ON DELETE CASCADE,
    system_key VARCHAR(40) UNIQUE,
    name       VARCHAR(40) NOT NULL,
    color      VARCHAR(7)  NOT NULL,
    kind       VARCHAR(16) NOT NULL
);
CREATE INDEX idx_categories_user ON categories (user_id);

CREATE TABLE merchant_rules (
    id          UUID PRIMARY KEY,
    user_id     UUID REFERENCES users (id) ON DELETE CASCADE,
    pattern     VARCHAR(120) NOT NULL,
    category_id UUID         NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ  NOT NULL
);
CREATE UNIQUE INDEX uq_merchant_rules_user_pattern ON merchant_rules (user_id, pattern) WHERE user_id IS NOT NULL;
CREATE UNIQUE INDEX uq_merchant_rules_global_pattern ON merchant_rules (pattern) WHERE user_id IS NULL;

CREATE TABLE import_profiles (
    id                 UUID PRIMARY KEY,
    user_id            UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name               VARCHAR(60) NOT NULL,
    delimiter          VARCHAR(1)  NOT NULL,
    date_column        VARCHAR(80) NOT NULL,
    date_format        VARCHAR(40) NOT NULL,
    description_column VARCHAR(80) NOT NULL,
    amount_column      VARCHAR(80),
    debit_column       VARCHAR(80),
    credit_column      VARCHAR(80),
    currency_column    VARCHAR(80),
    default_currency   VARCHAR(3),
    decimal_separator  VARCHAR(1)  NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_import_profiles_name UNIQUE (user_id, name)
);

CREATE TABLE import_batches (
    id                UUID PRIMARY KEY,
    user_id           UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    file_name         VARCHAR(255) NOT NULL,
    format_name       VARCHAR(80)  NOT NULL,
    rows_read         INT          NOT NULL DEFAULT 0,
    rows_imported     INT          NOT NULL DEFAULT 0,
    rows_duplicate    INT          NOT NULL DEFAULT 0,
    rows_skipped      INT          NOT NULL DEFAULT 0,
    rows_needs_review INT          NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_import_batches_user_created ON import_batches (user_id, created_at DESC);

CREATE TABLE transactions (
    id                    UUID PRIMARY KEY,
    user_id               UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    import_batch_id       UUID REFERENCES import_batches (id) ON DELETE SET NULL,
    booking_date          DATE           NOT NULL,
    amount                NUMERIC(14, 2) NOT NULL,
    currency              VARCHAR(3)     NOT NULL,
    description           VARCHAR(255)   NOT NULL,
    merchant              VARCHAR(120)   NOT NULL,
    category_id           UUID REFERENCES categories (id) ON DELETE SET NULL,
    categorization_source VARCHAR(10)    NOT NULL,
    confidence            NUMERIC(3, 2),
    needs_review          BOOLEAN        NOT NULL DEFAULT FALSE,
    fingerprint           VARCHAR(64)    NOT NULL,
    created_at            TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uq_transactions_fingerprint UNIQUE (user_id, fingerprint)
);
-- Access paths: list by date, aggregate by category and period, merchant re-categorisation, review queue.
CREATE INDEX idx_transactions_user_date ON transactions (user_id, booking_date DESC);
CREATE INDEX idx_transactions_user_category_date ON transactions (user_id, category_id, booking_date);
CREATE INDEX idx_transactions_user_merchant ON transactions (user_id, merchant);
CREATE INDEX idx_transactions_review ON transactions (user_id) WHERE needs_review;
CREATE INDEX idx_transactions_waiting ON transactions (created_at) WHERE category_id IS NULL AND categorization_source = 'NONE';

CREATE TABLE budgets (
    id            UUID PRIMARY KEY,
    user_id       UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id   UUID           NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    monthly_limit NUMERIC(14, 2) NOT NULL CHECK (monthly_limit > 0),
    currency      VARCHAR(3)     NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uq_budgets_category_currency UNIQUE (user_id, category_id, currency)
);

CREATE TABLE budget_warnings (
    id                     UUID PRIMARY KEY,
    budget_id              UUID NOT NULL REFERENCES budgets (id) ON DELETE CASCADE,
    warned_on              DATE NOT NULL,
    projected_overrun_date DATE,
    CONSTRAINT uq_budget_warnings_day UNIQUE (budget_id, warned_on)
);

CREATE TABLE conversations (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title      VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_conversations_user_updated ON conversations (user_id, updated_at DESC);

CREATE TABLE messages (
    id                     UUID PRIMARY KEY,
    conversation_id        UUID        NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    role                   VARCHAR(10) NOT NULL,
    content                TEXT        NOT NULL,
    interpretation         JSONB,
    tool_calls             JSONB,
    source_transaction_ids JSONB,
    created_at             TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_messages_conversation_created ON messages (conversation_id, created_at);

CREATE TABLE ai_usage (
    id            UUID PRIMARY KEY,
    user_id       UUID REFERENCES users (id) ON DELETE SET NULL,
    purpose       VARCHAR(20) NOT NULL,
    input_tokens  INT         NOT NULL,
    output_tokens INT         NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_ai_usage_created ON ai_usage (created_at);
