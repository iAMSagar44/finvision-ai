package dev.sagar.cash_flow_analyser.views.batch_job_services;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.vaadin.firitin.components.messagelist.MarkdownMessage;

import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("batch-job")
@PageTitle("Batch Job Assistant")
@Menu(title = "Batch Job Assistant", order = 3)
@Uses(Icon.class)
public class BatchJobView extends VerticalLayout {

  @Value("classpath:/prompts/batch-job-prompt.st")
  private Resource systemPromptResource;

  public BatchJobView(BatchJobService batchJobService, ChatMemory memory,
      ChatClient.Builder builder) {
    var chatClient = builder.defaultTools(batchJobService)
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build()).build();

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
      messageList.add(assistantMessage);
      chatClient.prompt()
          .system(s -> s.text(systemPromptResource).param("date",
              LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))))
          .user(event.getValue()).stream().content()
          .subscribe(assistantMessage::appendMarkdownAsync);
    });

    addAndExpand(scroller);
    add(messageInput);
  }

}
