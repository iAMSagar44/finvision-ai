package dev.sagar.batch_job_mcp.job;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
class CategorizeTransactionProcessor implements ItemProcessor<FinancialTransaction, FinancialTransaction> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
		.getLogger(CategorizeTransactionProcessor.class);

	private final ChatClient chatClient;

	private final CategoryService categoryService;

	private static final String SYSTEM_PROMPT = """
			You are a financial assistant. You will be given a transaction and you need to categorize it into one of the following categories: {categories}.
			Please provide the category in a single word.
			If you are unsure about the category, use "Other".
			""";

	public CategorizeTransactionProcessor(ChatClient.Builder builder, CategoryService categoryService) {
		this.chatClient = builder.build();
		this.categoryService = categoryService;
	}

	@Override
	public FinancialTransaction process(FinancialTransaction transaction) throws Exception {
		var category = chatClient.prompt()
			.system(systemMessage -> systemMessage.text(SYSTEM_PROMPT)
				.param("categories", categoryService.getCategories()))
			.user(String.format("Categorize the transaction: %s", transaction.transaction_detail()))
			.call()
			.content();
		logger.debug("Transaction: {} -> Category: {}", transaction.transaction_detail(), category);

		return new FinancialTransaction(transaction.date(), Math.abs(transaction.amount()),
				transaction.transaction_detail(), category, transaction.transaction_type());
	}

}
