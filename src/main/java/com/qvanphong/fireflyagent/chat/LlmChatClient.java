package com.qvanphong.fireflyagent.chat;

import com.qvanphong.fireflyagent.pojo.CompletionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

@RequiredArgsConstructor
@Component
@Slf4j
public class LlmChatClient {
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final ChatClient chatClient;
    private final ToolCallingManager toolCallingManager;
    private final ChatMemory chatMemory;

    private String llmSystemPrompt;


    public CompletionInfo completion(String content) {
        Prompt prompt = builtPrompt(content);

        ChatResponse chatResponse = chatClient
                .prompt(prompt)
                .call()
                .chatResponse();

        CompletionInfo completionInfo = new CompletionInfo();
        while (chatResponse != null && chatResponse.hasToolCalls()) {
            List<AssistantMessage.ToolCall> toolsCalled = getToolCalls(chatResponse);
            completionInfo.addToolCalled(toolsCalled);

            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, chatResponse);
            prompt = new Prompt(toolExecutionResult.conversationHistory(), prompt.getOptions());

            chatResponse = chatClient
                    .prompt(prompt)
                    .call()
                    .chatResponse();
        }

        AssistantMessage assistantMessage = Optional.ofNullable(chatResponse)
                .map(ChatResponse::getResult)
                .map(Generation::getOutput)
                .orElse(AssistantMessage.builder()
                        .content("ok")
                        .toolCalls(ObjectUtils.defaultIfNull(completionInfo.getToolsCalled(), Collections.emptyList()))
                        .build());

        completionInfo.setResponseMessage(assistantMessage.getText());

        chatMemory.add("noop", prompt.getUserMessage());
        chatMemory.add("noop", assistantMessage);
        return completionInfo;
    }

    private List<AssistantMessage.ToolCall> getToolCalls(ChatResponse chatResponse) {
        return chatResponse.getResults()
                .stream()
                .filter(generation -> generation.getOutput().hasToolCalls())
                .map(generation -> generation.getOutput().getToolCalls())
                .peek(toolCalls -> {
                    if (log.isDebugEnabled()) {
                        for (AssistantMessage.ToolCall toolCall : toolCalls) {
                            log.debug("{} - payload: {}", toolCall.name(), toolCall.arguments());
                        }
                    }
                })
                .flatMap(List::stream)
                .toList();
    }

    private Prompt builtPrompt(String content) {
        ToolCallingChatOptions chatOptions = ToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(false)
                .build();

        UserMessage userMessage = new UserMessage(content);
        return new Prompt(List.of(userMessage, buildSystemMessage()), chatOptions);
    }

    private Message buildSystemMessage() {
        String currentTime = simpleDateFormat.format(new Date());

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(llmSystemPrompt);
        return systemPromptTemplate.createMessage(Map.of("currentTime", currentTime));
    }

    @Qualifier("llmSystemPrompt")
    @Autowired
    public void setLlmSystemPrompt(String llmSystemPrompt) {
        this.llmSystemPrompt = llmSystemPrompt;
    }
}
