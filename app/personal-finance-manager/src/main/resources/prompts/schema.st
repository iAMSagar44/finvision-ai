-- Table: financial_transaction. This table contains both debit and credit transactions from a user's bank account.
CREATE TABLE IF NOT EXISTS financial_transactions(
    amount double precision NOT NULL,
    "date" date NOT NULL,
    id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
    category varchar(255) NOT NULL,
    transaction_detail varchar(255) NOT NULL,
    transaction_type varchar(255) NOT NULL DEFAULT 'DEBIT'::character varying,
    PRIMARY KEY(id)
);