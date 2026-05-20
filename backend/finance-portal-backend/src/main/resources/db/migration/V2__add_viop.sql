-- viop_instrument tablosu oluştur
CREATE TABLE IF NOT EXISTS viop_instrument (
                                               id BIGINT PRIMARY KEY REFERENCES base_instrument(id),
    underlying_asset VARCHAR(20),
    contract_type VARCHAR(10) DEFAULT 'FUTURES',
    expiry_date DATE,
    initial_margin NUMERIC(18, 2)
    );