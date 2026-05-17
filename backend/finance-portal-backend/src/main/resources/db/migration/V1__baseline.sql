-- ===============================
-- BASE INSTRUMENT (Ana tablo)
-- ===============================
CREATE TABLE IF NOT EXISTS base_instrument (
                                               id BIGSERIAL PRIMARY KEY,
                                               symbol VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    exchange VARCHAR(50),
    currency VARCHAR(10),
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
    );

-- ===============================
-- INSTRUMENT ALT TABLOLARI
-- ===============================
CREATE TABLE IF NOT EXISTS stock_instrument (
                                                id BIGINT PRIMARY KEY REFERENCES base_instrument(id),
    sector VARCHAR(50),
    market_cap NUMERIC(18,2)
    );

CREATE TABLE IF NOT EXISTS forex_instrument (
                                                id BIGINT PRIMARY KEY REFERENCES base_instrument(id),
    base_currency VARCHAR(10),
    quote_currency VARCHAR(10)
    );

CREATE TABLE IF NOT EXISTS crypto_instrument (
                                                 id BIGINT PRIMARY KEY REFERENCES base_instrument(id),
    blockchain VARCHAR(50),
    total_supply NUMERIC(25,8),
    circulating_supply NUMERIC(25,8)
    );

CREATE TABLE IF NOT EXISTS bond_instrument (
                                               id BIGINT PRIMARY KEY REFERENCES base_instrument(id),
    maturity_date DATE,
    coupon_rate NUMERIC(5,2),
    face_value NUMERIC(10,2),
    issuer VARCHAR(50)
    );

CREATE TABLE IF NOT EXISTS precious_instrument (
                                                   id BIGINT PRIMARY KEY REFERENCES base_instrument(id),
    metal_type VARCHAR(20),
    unit VARCHAR(10)
    );

CREATE TABLE IF NOT EXISTS fund_instrument (
                                               id BIGINT PRIMARY KEY REFERENCES base_instrument(id),
    fund_code VARCHAR(50),
    fund_type VARCHAR(100),
    umbrella VARCHAR(100),
    total_value NUMERIC(18,6),
    investor_count INTEGER
    );

-- ===============================
-- INSTRUMENT PRICES
-- ===============================
CREATE TABLE IF NOT EXISTS instrument_prices (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 instrument_id BIGINT NOT NULL REFERENCES base_instrument(id),
    current_price NUMERIC(18,6) NOT NULL,
    open_price NUMERIC(18,6) NOT NULL,
    high_price NUMERIC(18,6) NOT NULL,
    low_price NUMERIC(18,6) NOT NULL,
    previous_close NUMERIC(18,6) NOT NULL,
    change_amount NUMERIC(18,6),
    change_percent NUMERIC(10,4),
    volume BIGINT,
    yield_rate NUMERIC(10,4),
    timestamp TIMESTAMP NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_instrument_timestamp ON instrument_prices(instrument_id, timestamp);

-- ===============================
-- PRICE HISTORY
-- ===============================
CREATE TABLE IF NOT EXISTS price_history (
                                             id BIGSERIAL PRIMARY KEY,
                                             instrument_id BIGINT NOT NULL REFERENCES base_instrument(id),
    date DATE NOT NULL,
    open NUMERIC(18,6) NOT NULL,
    high NUMERIC(18,6) NOT NULL,
    low NUMERIC(18,6) NOT NULL,
    close NUMERIC(18,6) NOT NULL,
    volume BIGINT,
    yield_rate NUMERIC(10,4)
    );

CREATE INDEX IF NOT EXISTS idx_instrument_date ON price_history(instrument_id, date);

-- ===============================
-- USERS
-- ===============================
CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     keycloak_id VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    password_last_changed TIMESTAMP,
    theme VARCHAR(10) DEFAULT 'light',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    notify_transaction BOOLEAN NOT NULL DEFAULT TRUE,
    notify_portfolio_change BOOLEAN NOT NULL DEFAULT TRUE,
    notify_price_alert BOOLEAN NOT NULL DEFAULT TRUE,
    notify_news BOOLEAN NOT NULL DEFAULT TRUE
    );

-- ===============================
-- TOTP SECRETS
-- ===============================
CREATE TABLE IF NOT EXISTS totp_secrets (
                                            id BIGSERIAL PRIMARY KEY,
                                            keycloak_id VARCHAR(255) NOT NULL UNIQUE,
    secret VARCHAR(255) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE
    );

-- ===============================
-- EMAIL VERIFICATION TOKENS
-- ===============================
CREATE TABLE IF NOT EXISTS email_verification_tokens (
                                                         id BIGSERIAL PRIMARY KEY,
                                                         token VARCHAR(255) NOT NULL UNIQUE,
    keycloak_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
    );

-- ===============================
-- WATCHLIST
-- ===============================
CREATE TABLE IF NOT EXISTS watchlist (
                                         id BIGSERIAL PRIMARY KEY,
                                         user_id VARCHAR(255) NOT NULL,
    instrument_id BIGINT NOT NULL REFERENCES base_instrument(id),
    added_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_user_instrument UNIQUE (user_id, instrument_id)
    );

-- ===============================
-- PRICE ALERTS
-- ===============================
CREATE TABLE IF NOT EXISTS price_alerts (
                                            id BIGSERIAL PRIMARY KEY,
                                            user_id VARCHAR(255) NOT NULL,
    instrument_id BIGINT NOT NULL REFERENCES base_instrument(id),
    target_price NUMERIC(20,6) NOT NULL,
    condition VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    triggered BOOLEAN NOT NULL DEFAULT FALSE,
    triggered_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
    );

-- ===============================
-- NOTIFICATIONS
-- ===============================
CREATE TABLE IF NOT EXISTS notifications (
                                             id BIGSERIAL PRIMARY KEY,
                                             user_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(500) NOT NULL,
    type VARCHAR(50) NOT NULL,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    related_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL
    );

-- ===============================
-- NEWS
-- ===============================
CREATE TABLE IF NOT EXISTS news (
                                    id BIGSERIAL PRIMARY KEY,
                                    title VARCHAR(255) NOT NULL,
    content TEXT,
    category VARCHAR(255),
    source VARCHAR(255),
    image_url VARCHAR(255),
    published_at TIMESTAMP
    );

-- ===============================
-- PORTFOLIOS
-- ===============================
CREATE TABLE IF NOT EXISTS portfolios (
                                          id BIGSERIAL PRIMARY KEY,
                                          user_id VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    portfolio_type VARCHAR(20) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
    );

-- ===============================
-- PORTFOLIO HOLDINGS
-- ===============================
CREATE TABLE IF NOT EXISTS portfolio_holdings (
                                                  id BIGSERIAL PRIMARY KEY,
                                                  portfolio_id BIGINT NOT NULL,
                                                  instrument_id BIGINT NOT NULL,
                                                  quantity NUMERIC(18,8) NOT NULL,
    average_buy_price NUMERIC(18,6) NOT NULL,
    currency VARCHAR(10),
    exchange_rate NUMERIC(18,6),
    first_purchase_date TIMESTAMP NOT NULL,
    last_purchase_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uk_portfolio_instrument UNIQUE (portfolio_id, instrument_id),
    CONSTRAINT fk_holding_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios(id),
    CONSTRAINT fk_holding_instrument FOREIGN KEY (instrument_id) REFERENCES base_instrument(id)
    );

-- ===============================
-- PORTFOLIO TRANSACTIONS
-- ===============================
CREATE TABLE IF NOT EXISTS portfolio_transactions (
                                                      id BIGSERIAL PRIMARY KEY,
                                                      portfolio_id BIGINT NOT NULL,
                                                      instrument_id BIGINT NOT NULL,
                                                      transaction_type VARCHAR(10) NOT NULL,
    quantity NUMERIC(18,8) NOT NULL,
    price NUMERIC(18,6) NOT NULL,
    total_amount NUMERIC(18,2) NOT NULL,
    currency VARCHAR(10),
    exchange_rate NUMERIC(18,6),
    commission NUMERIC(18,2),
    tax NUMERIC(18,2),
    transaction_date TIMESTAMP NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_transaction_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios(id),
    CONSTRAINT fk_transaction_instrument FOREIGN KEY (instrument_id) REFERENCES base_instrument(id)
    );

CREATE INDEX IF NOT EXISTS idx_transaction_portfolio ON portfolio_transactions(portfolio_id);
CREATE INDEX IF NOT EXISTS idx_transaction_date ON portfolio_transactions(transaction_date);