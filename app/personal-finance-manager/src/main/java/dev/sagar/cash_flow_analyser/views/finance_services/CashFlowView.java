package dev.sagar.cash_flow_analyser.views.finance_services;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.ai.util.json.JsonParser;
import org.vaadin.firitin.components.messagelist.MarkdownMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.UserMessage;

@Route("")
@PageTitle("Finance Assistant")
@Menu(title = "Personal Finance Assistant", order = 1)
@Uses(Icon.class)
public class CashFlowView extends VerticalLayout {
  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(CashFlowView.class);

  private final FrontDeskAgent frontDeskService;
  private final StreamingChatModel chatModel;
  private final ChatMemoryService chatMemoryService;

  private static final String SYSTEM_PROMPT =
      """
          You are a polite, professional assistant specializing in Personal Finance data analysis. Provide clear, concise explanations and insights.
          Today's date is {date}.

          When handling financial questions:
          1. First use the 'analyseUserQuestion' to rewrite the question for better results.
              IMPORTANT: Pass the user's original question to the 'analyseUserQuestion' tool for analysis. DO NOT attempt to rewrite the question yourself.
              This will help in understanding the user's intent and context.
              IMPORTANT: If the analysis returns a valid answer, provide it directly to the user without further processing.
              If the analysis does not return a valid answer, proceed with the next steps.
          2. Use the 'generateQuery' tool to handle SQL query generation based on the rewritten question.
          3. ALWAYS validate the generated SQL query before executing it. Use the 'evaluateQuery' tool for validating the SQL query
              If any issues or improvements are identified during validation or execution, re-generate the query based on the feedback by using the 'generateQuery' tool.
              Retry up to 3 times, refining the query each time based on the latest feedback.
              If the query still fails after 3 attempts, return the most improved version along with the final feedback or error message.
          4. Once the query is validated and only if the validation is a PASS, execute it using the 'getAggregatedData' tool for data queries, defaulting to aggregated data unless a detailed breakdown is requested.
          5. Present results in a clear, organized format with insightful analysis.

          Follow these guidelines:
          - Never generate SQL queries yourself - always use the 'generateQuery' tool to generate the SQL query for the user's question.
          - ALWAYS validate the generated SQL query before executing it.
          - DO NOT exectute the query before validating the query.
          - Present results in HTML tables by default for data inquiries.
          - Always present tables in HTML format and not in markdown.
          - Only provide detailed transaction breakdowns if specifically requested.
          - Only list individual transactions if explicitly requested by the user.
          - If a query returns no results, indicate this clearly and ask for clarification.
          - If query generation or execution fails after 3 retry attempts, politely ask the user to refine their question.
          - Analyze results to provide meaningful insights beyond just displaying data.
          - Remember that all financial values are in AUD (Australian Dollar).

          For any query that returns data, provide:
          1. A clear response to the user's question
          2. Key insights from the data
          3. Brief context or patterns identified in the financial information

          Be concise but thorough, professional but approachable, and always prioritize accuracy in financial analysis.

          """;

  public CashFlowView(QueryAnalyserAgent queryAnalyserService,
      SQLQueryAgent sqlQueryService, DatabaseService databaseService,
      SQLEvaluatorAgent evaluatorService, ChatMemory memory,
      FrontDeskAgent frontDeskService, StreamingChatModel chatModel) {

    this.chatModel = chatModel;
    this.chatMemoryService = new ChatMemoryService(memory, 5);
    ToolCallback[] databaseTools = ToolCallbacks.from(queryAnalyserService,
        sqlQueryService, evaluatorService, databaseService);

    ChatOptions chatOptions = ToolCallingChatOptions.builder()
        .toolCallbacks(databaseTools).internalToolExecutionEnabled(false).build();

    this.frontDeskService = frontDeskService;

    var messageList = new VerticalLayout();
    var scroller = new Scroller(messageList);
    var messageInput = new MessageInput();
    messageInput.setWidthFull();
    scroller.setWidthFull();
    messageList.setWidthFull();

    messageInput.addSubmitListener(event -> {
      var userMessage = event.getValue();
      var assistantMessage = new MarkdownMessage("Assistant");
      messageList.add(new MarkdownMessage(userMessage, "You"), assistantMessage);

      SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(SYSTEM_PROMPT);
      var systemMessage = systemPromptTemplate.createMessage(Map.of("date",
          LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))));

      Prompt prompt =
          new Prompt(List.of(systemMessage, new UserMessage(userMessage)), chatOptions);


      Flux<ChatResponse> chatResponse = chatModel.stream(prompt);

      Flux<String> chatMessageStream =
          processStream(chatResponse, prompt).subscribeOn(Schedulers.boundedElastic());

      Flux<String> processedStream =
          Flux.mergeSequential(this.frontDeskService.acknowledgeQuestion(userMessage)
              .subscribeOn(Schedulers.boundedElastic()), chatMessageStream);

      // Use the ChatMemoryService to handle the assistant message
      processedStream = chatMemoryService.addMessages(processedStream, userMessage);

      // Set up the UI update subscription
      processedStream.subscribe(assistantMessage::appendMarkdownAsync);
    });

    addAndExpand(scroller);
    add(messageInput);
  }

  private Flux<String> processStream(Flux<ChatResponse> chatResponse,
      Prompt initialPrompt) {
    ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();

    return chatResponse.flatMap(response -> handleToolCallsRecursively(response,
        initialPrompt, toolCallingManager));
  }

  private Flux<String> handleToolCallsRecursively(ChatResponse response, Prompt prompt,
      ToolCallingManager toolCallingManager) {
    if (!response.hasToolCalls()) {
      logger.trace("No tool calls found in the response");
      Flux<String> chatResponseStream = Flux
          .just((response.getResult() == null || response.getResult().getOutput() == null
              || response.getResult().getOutput().getText() == null) ? ""
                  : response.getResult().getOutput().getText());
      return chatResponseStream;
    }

    // Log tool calls
    List<ToolCall> toolCalls = response.getResult().getOutput().getToolCalls();
    logger.info("Tool calls {}", toolCalls);

    // Emit tool call info + defer tool execution
    return Flux.concat(Flux.fromIterable(toolCalls).map(toolCall -> {

      if (toolCall.name().equals("analyseUserQuestion")) {
        logger.trace("LLM is invoking the queryAnalyserService");
        return """
            <small>
              <i>Running tool %s to analyse the question:</i>
              <br>
              <details>
                <summary>View tool arguments</summary>
                <pre style="white-space: pre-wrap;">%s</pre>
              </details>
            </small>
            """.formatted(toolCall.name(), toolCall.arguments());
      } else if (toolCall.name().equals("generateQuery")) {
        var arguments = toolCall.arguments();
        // The first run of the tool call will have feedback as empty
        // so removing it and the sqlQuery from the arguments
        // to avoid sending it to the client
        Map<String, Object> json =
            JsonParser.fromJson(arguments, new TypeReference<>() {});
        if (json.get("feedback") instanceof String feedback
            && (feedback.isBlank() || feedback.isEmpty())) {
          json.remove("feedback");
          json.remove("sqlQuery");
          arguments = JsonParser.toJson(json);
        }

        logger.trace("LLM is invoking the sqlQueryService");
        return """
            <small>
              <i>Running tool %s to generate a data query:</i>
              <br>
              <details>
                <summary>View tool arguments</summary>
                <pre style="white-space: pre-wrap;">%s</pre>
              </details>
            </small>
            """.formatted(toolCall.name(), arguments);
      } else if (toolCall.name().equals("evaluateQuery")) {
        logger.trace("LLM is invoking the evaluatorService");
        return """
            <small>
              <i>Running tool %s to evaluate the data query, ensuring it is valid:</i>
              <br>
              <details>
                <summary>View tool arguments</summary>
                <pre style="white-space: pre-wrap;">%s</pre>
              </details>
            </small>
            """.formatted(toolCall.name(), toolCall.arguments());
      } else if (toolCall.name().equals("getAggregatedData")) {
        logger.trace("LLM is invoking the databaseService");
        return """
            <small>
              <i>Running tool %s to execute the query and analyse the data:</i>
              <br>
              <details>
                <summary>View tool arguments</summary>
                <pre style="white-space: pre-wrap;">%s</pre>
              </details>
            </small>
            <br>
            """.formatted(toolCall.name(), toolCall.arguments());
      } else {
        logger.info("AI is calling tool: {}", toolCall.name());
        return "Retrieving tool: " + toolCall.name();
      }
    }), Flux.defer(() -> {
      ToolExecutionResult result = toolCallingManager.executeToolCalls(prompt, response);

      result.conversationHistory().forEach(m -> logger.debug("Message: {}", m));

      Prompt newPrompt = new Prompt(result.conversationHistory(), prompt.getOptions());

      // Re-stream using new prompt and recurse
      return chatModel.stream(newPrompt)
          .flatMap(nextResponse -> handleToolCallsRecursively(nextResponse, newPrompt,
              toolCallingManager));
    }).subscribeOn(Schedulers.boundedElastic()));
  }

}
