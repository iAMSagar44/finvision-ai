package dev.sagar.cash_flow_analyser.views.batch_job_services;

import javax.sql.DataSource;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.jdbc.core.simple.JdbcClient;
import dev.sagar.cash_flow_analyser.dto.FinancialTransaction;

class FilterCreditCardPaymentsProcessor
    implements ItemProcessor<FinancialTransaction, FinancialTransaction> {

  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(FilterCreditCardPaymentsProcessor.class);

  private final JdbcClient jdbcClient;
  private final String fileName;

  public FilterCreditCardPaymentsProcessor(DataSource dataSource, String fileName) {
    this.jdbcClient = JdbcClient.create(dataSource);
    this.fileName = fileName;
  }

  @Override
  public FinancialTransaction process(FinancialTransaction transaction) throws Exception {
    if (transaction.transaction_detail().toLowerCase().contains("hsbc")) {
      logger.info(
          "Filtering credit card payment transaction and updating the Credit Card Payments table: {}",
          transaction);
      jdbcClient.sql(
          "INSERT INTO credit_card_payments (amount, date, transaction_detail, source_file) VALUES (?, ?, ?, ?)")
          .param(transaction.amount()).param(transaction.date())
          .param(transaction.transaction_detail()).param(fileName).update();
      return null; // Filter out transactions with "HSBC" in the transaction detail.
                   // These are
                   // credit card payments and expenses around it are recorded in
                   // the database.
    } else {
      return transaction; // Keep other transactions.
    }
  }

}
