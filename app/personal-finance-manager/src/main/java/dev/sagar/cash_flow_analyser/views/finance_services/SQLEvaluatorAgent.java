package dev.sagar.cash_flow_analyser.views.finance_services;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class SQLEvaluatorAgent {

    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(SQLEvaluatorAgent.class);

    @Value("classpath:/prompts/schema.st")
    private Resource ddlResource;

    @Value("classpath:/prompts/sql-evaluator-prompt.st")
    private Resource systemPromptTemplate;

    @Value("${app.database.type}")
    private String databaseType;

    private final DatabaseService databaseService;

    private static final String USER_MESSAGE_TEMPLATE =
            """
                    For the given question and database table schema, validate the SQL query and provide feedback.
                                    The user's question is -
                                    {question}

                                    The Table Schema is -
                                    {ddl}

                                    The transaction_type column is restricted to only two possible values: 'DEBIT' and 'CREDIT'.

                                    Possible list of Categories -
                                    {categories}

                                    The SQL query is -
                                    {sql_query}

                                    Ensure the transaction_detail and category columns are converted to lowercase for case-insensitive matching.

                                    Today's date is {current_date}

                                    CRITICAL - The SQL query should be compatible with the given schema and must be compatible with {database_type} and dialect.

                                            """;

    private final ChatClient chatClient;

    public SQLEvaluatorAgent(ChatClient.Builder builder, DatabaseService databaseService) {
        this.chatClient = builder.defaultOptions(OpenAiChatOptions.builder()
                .model(OpenAiApi.ChatModel.O4_MINI).temperature(1D).build()).build();
        this.databaseService = databaseService;
    }


    @Tool(description = "Validate the generated SQL query based on the user's question")
    EvaluationResponse evaluateQuery(
            @ToolParam(description = "The rewritten question") String userQuestion,
            @ToolParam(description = "The generated SQL query to validate") String sqlQuery)
            throws IOException {

        String schema = ddlResource.getContentAsString(Charset.defaultCharset());

        logger.trace("Retrieving categories from the database.");
        var categories = String.join(", ", databaseService.getCategories());

        var evaluationResponse = chatClient.prompt()
                .system(systemSpec -> systemSpec.text(systemPromptTemplate).param("database_type",
                        databaseType))
                .user(userSpec -> userSpec.text(USER_MESSAGE_TEMPLATE)
                        .param("question", userQuestion).param("ddl", schema)
                        .param("categories", categories).param("sql_query", sqlQuery)
                        .param("current_date",
                                LocalDate.now().format(
                                        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)))
                        .param("database_type", databaseType))
                .call().entity(EvaluationResponse.class);

        logger.debug("Evaluation: {} \n Feedback: {}", evaluationResponse.evaluation(),
                evaluationResponse.feedback());

        return evaluationResponse;

    }

}
