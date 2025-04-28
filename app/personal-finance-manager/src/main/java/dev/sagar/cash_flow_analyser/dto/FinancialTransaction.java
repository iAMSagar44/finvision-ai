package dev.sagar.cash_flow_analyser.dto;

import java.time.LocalDate;

public record FinancialTransaction(LocalDate date, Double amount,
    String transaction_detail, String category, TransactionType transaction_type) {

  public FinancialTransaction(LocalDate date, Double amount, String description) {
    this(date, amount, description, "", TransactionType.DEBIT);
  }

  public FinancialTransaction(LocalDate date, Double amount, String transaction_detail,
      TransactionType transaction_type) {
    this(date, amount, transaction_detail, "", transaction_type);
  }

  public FinancialTransaction(LocalDate date, Double amount, String transaction_detail,
      String category, TransactionType transaction_type) {
    this.date = date;
    this.amount = amount;
    this.transaction_detail = transaction_detail;
    this.category = category;
    this.transaction_type = transaction_type;
  }

}
