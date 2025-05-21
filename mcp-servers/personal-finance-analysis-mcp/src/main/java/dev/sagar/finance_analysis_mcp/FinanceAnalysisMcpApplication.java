package dev.sagar.finance_analysis_mcp;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FinanceAnalysisMcpApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinanceAnalysisMcpApplication.class, args);
	}

	@Bean
	OpenAiChatModel openAiChatModel() {
		var openAiApi = OpenAiApi.builder().apiKey(System.getenv("OPENAI_API_KEY")).build();
		return OpenAiChatModel.builder().openAiApi(openAiApi).build();
	}

	@Bean
	public ToolCallbackProvider financeAnalysisTools(
			PersonalFinanceService personalFinanceService) {
		return MethodToolCallbackProvider.builder().toolObjects(personalFinanceService).build();
	}
}
