package dev.sagar.batch_job_mcp.job;

import javax.sql.DataSource;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.jdbc.core.simple.JdbcClient;


class FilterCreditTransactionsProcessor
		implements ItemProcessor<FinancialTransaction, FinancialTransaction> {

	private static final org.slf4j.Logger logger =
			org.slf4j.LoggerFactory.getLogger(FilterCreditTransactionsProcessor.class);

	private final JdbcClient jdbcClient;
	private final String fileName;

	public FilterCreditTransactionsProcessor(DataSource dataSource, String fileName) {
		this.jdbcClient = JdbcClient.create(dataSource);
		this.fileName = fileName;
	}

	@Override
	public FinancialTransaction process(FinancialTransaction transaction) throws Exception {
		if (transaction.amount() > 0) {
			logger.info("Filtering credit transaction and updating the credits table");
			jdbcClient.sql(
					"INSERT INTO credit_transactions (amount, date, transaction_detail, source_file) VALUES (?, ?, ?, ?)")
					.param(transaction.amount()).param(transaction.date())
					.param(transaction.transaction_detail()).param(fileName).update();
			return new FinancialTransaction(transaction.date(), transaction.amount(),
					transaction.transaction_detail(), TransactionType.CREDIT); // These are credit
																				// transactions.
		} else {
			return transaction; // Keep negative amounts, these are debit transactions.
		}
	}

}
