package dev.sagar.cash_flow_analyser.views.finance_services;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@Component
class QueryAnalyserAgent {
    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(QueryAnalyserAgent.class);

    private static final String USER_PROMPT =
            """
                    Given a user query, first check the MEMORY section to determine if there's a relevant answer.

                    If an answer is found in MEMORY:
                    - Return only that information, citing it as coming from memory
                    - Do not elaborate beyond what's explicitly stated in the memory
                    - If multiple relevant answers exist, return the most comprehensive one

                    If the user insists to recheck or answer again:
                    - DO NOT check the MEMORY section
                    - DO NOT return the answer from the MEMORY section
                    - DO NOT attempt to generate an answer
                    - Respond with: "No answer found."

                    If NO answer is found in MEMORY:
                    - Clearly state "I don't have enough information to answer this question"
                    - DO NOT attempt to generate an answer
                    - DO NOT provide speculative information
                    - You may rewrite the query to be more specific and concise for future database searches

                    When rewriting queries:
                    - Remove irrelevant information
                    - Make the query concise and specific
                    - If the query is already well-formed, return it unchanged
                    - Do not add information beyond the original query

                    Today's date is {date}.

                    Original query:
                    {query}
                    ---------------------
                    MEMORY:
                    {memory}
                    ---------------------
                    """;


    private final ChatClient chatClient;
    private final ChatMemoryService chatMemoryService;

    public QueryAnalyserAgent(ChatClient.Builder chatClientBuilder, ChatMemory memory) {
        this.chatClient =
                chatClientBuilder
                        .defaultOptions(OpenAiChatOptions.builder()
                                .model(OpenAiApi.ChatModel.O4_MINI).temperature(1D).build())
                        .build();
        this.chatMemoryService = new ChatMemoryService(memory, 5);
    }

    @Tool(description = """
            Analyzes the user question and rewrites it to provide better results when querying a database.
            Checks if the answer is already present in the conversation context and returns it if found.
            """)
    public QueryAnalyserResponse analyseUserQuestion(@ToolParam(
            description = "The user's original question to analyze") String userQuestion) {
        QueryAnalyserResponse response = chatClient.prompt().user(userSpec -> userSpec
                .text(USER_PROMPT)
                .param("date",
                        LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)))
                .param("query", userQuestion)
                .param("memory", chatMemoryService.getConversationMessages())).call()
                .entity(QueryAnalyserResponse.class);
        logger.info("Query Analyser Response: {}", response);

        return response;
    }

    record QueryAnalyserResponse(
            @JsonPropertyDescription(
                    value = "The rewritten query based on user input") String reWrittenQuery,
            @JsonPropertyDescription(
                    value = "Indicates if an answer was found in the conversation context") boolean foundAnswer,
            @JsonPropertyDescription(
                    value = "The answer from the conversation context") String answer) {
    }

}
