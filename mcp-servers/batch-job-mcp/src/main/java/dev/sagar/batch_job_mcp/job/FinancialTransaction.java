package dev.sagar.batch_job_mcp.job;

import java.time.LocalDate;

record FinancialTransaction(
                LocalDate date,
                Double amount,
                String transaction_detail,
                String category,
                TransactionType transaction_type) {

        FinancialTransaction(LocalDate date, Double amount, String description) {
                this(date, amount, description, "", TransactionType.DEBIT);
        }

        FinancialTransaction(LocalDate date, Double amount, String transaction_detail,
                        TransactionType transaction_type) {
                this(date, amount, transaction_detail, "", transaction_type);
        }

        FinancialTransaction(LocalDate date, Double amount, String transaction_detail, String category,
                        TransactionType transaction_type) {
                this.date = date;
                this.amount = amount;
                this.transaction_detail = transaction_detail;
                this.category = category;
                this.transaction_type = transaction_type;
        }

}