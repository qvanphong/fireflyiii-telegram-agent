package com.qvanphong.fireflyagent.chat;

import com.qvanphong.fireflyagent.pojo.CompletionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class LlmChatClient {
//    private final ChatModel chatModel;
    private final ChatClient chatClient;
    private final ToolCallingManager toolCallingManager;
    private final ToolCallbackProvider toolCallbackProvide;


    public CompletionInfo chat(String content) {
//       return  chatModel.call(content);
        ToolCallingChatOptions chatOptions = ToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(false)
                .build();

        Prompt prompt = new Prompt(content, chatOptions);
        ChatResponse chatResponse = chatClient
                .prompt(prompt)
                .call()
                .chatResponse();

        CompletionInfo completionInfo = new CompletionInfo();
        while (chatResponse != null && chatResponse.hasToolCalls()) {
            List<AssistantMessage.ToolCall> toolsCalled = chatResponse.getResults()
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
            completionInfo.addToolCall(toolsCalled);

            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, chatResponse);

            prompt = new Prompt(toolExecutionResult.conversationHistory(), chatOptions);

            chatResponse = chatClient
                    .prompt(prompt)
                    .call()
                    .chatResponse();
        }

        completionInfo.setResponseMessage(chatResponse == null ? "" : chatResponse.getResult().getOutput().getText());

        return completionInfo;
    }
}
