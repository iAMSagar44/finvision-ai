-- Table: credit_card_payments
CREATE TABLE IF NOT EXISTS credit_card_payments (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    "date" date NOT NULL,
    amount double precision NOT NULL,
    transaction_detail varchar(255) NOT NULL,
    insertion_date timestamp NOT NULL DEFAULT NOW(),
    source_file varchar(255) NOT NULL
);

-- Table: credit_transactions
CREATE TABLE IF NOT EXISTS credit_transactions (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    "date" date NOT NULL,
    amount double precision NOT NULL,
    transaction_detail varchar(255) NOT NULL,
    insertion_date timestamp NOT NULL DEFAULT NOW(),
    source_file varchar(255) NOT NULL
);

-- Table: financial_transactions
CREATE TABLE IF NOT EXISTS financial_transactions (
    amount double precision NOT NULL,
    "date" date NOT NULL,
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category varchar(255) NOT NULL,
    transaction_detail varchar(255) NOT NULL,
    transaction_type varchar(255) NOT NULL DEFAULT 'DEBIT'::character varying,
    insertion_date timestamp NOT NULL DEFAULT NOW(),
    source_file varchar(255) NOT NULL
);

-- Table: categories
CREATE TABLE IF NOT EXISTS categories(
    id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
    category varchar(255) NOT NULL UNIQUE,
    PRIMARY KEY(id)
);