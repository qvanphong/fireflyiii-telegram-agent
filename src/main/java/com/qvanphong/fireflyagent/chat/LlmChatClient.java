package com.qvanphong.fireflyagent.chat;

import com.qvanphong.fireflyagent.pojo.CompletionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

@RequiredArgsConstructor
@Component
@Slf4j
public class LlmChatClient {
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final int MAX_TOOL_CALL = 10;

    private final ChatClient chatClient;
    private final ToolCallingManager toolCallingManager;
    @Value("${telegram.bot.ownerId}")
    private String botOwnerId;

    private final ChatMemory globalChatMemory = MessageWindowChatMemory.builder().maxMessages(25).build();

    private String llmSystemPrompt;


    public CompletionInfo completion(String content) {
        CompletionInfo completionInfo = new CompletionInfo();

        try {
            Prompt prompt = builtInputPrompt(content);
            ChatResponse chatResponse = chatClient.prompt(prompt).call().chatResponse();

            chatResponse = executeToolsIfNeeded(chatResponse, prompt, completionInfo);

            AssistantMessage assistantMessage = Optional.ofNullable(chatResponse).map(ChatResponse::getResult).map(Generation::getOutput).orElse(AssistantMessage.builder().content("Done (LLM response with 0 content)").toolCalls(ObjectUtils.defaultIfNull(completionInfo.getToolsCalled(), Collections.emptyList())).build());

            completionInfo.setResponseMessage(assistantMessage.getText());
            globalChatMemory.add(botOwnerId, assistantMessage);
        } catch (Exception e) {
            completionInfo.setSuccess(true);
            completionInfo.setError(e.getMessage());
            globalChatMemory.add(botOwnerId, AssistantMessage.builder().content("LLM failed to process or no response.").build());
        }

        return completionInfo;
    }

    private ChatResponse executeToolsIfNeeded(ChatResponse chatResponse, Prompt inputPrompt, CompletionInfo completionInfo) {
        int calledTimes = 0;
        Prompt toolCallPrompt = inputPrompt;

        while (chatResponse != null && chatResponse.hasToolCalls() && calledTimes < MAX_TOOL_CALL) {
            List<AssistantMessage.ToolCall> toolsCalled = getToolCalls(chatResponse);
            completionInfo.addToolCalled(toolsCalled);

            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(toolCallPrompt, chatResponse);
            toolExecutionResult.conversationHistory();

            toolCallPrompt = new Prompt(toolExecutionResult.conversationHistory(), toolCallPrompt.getOptions());

            chatResponse = chatClient.prompt(toolCallPrompt).call().chatResponse();

            calledTimes++;
        }
        return chatResponse;
    }

    private List<AssistantMessage.ToolCall> getToolCalls(ChatResponse chatResponse) {
        return chatResponse.getResults().stream().filter(generation -> generation.getOutput().hasToolCalls()).map(generation -> generation.getOutput().getToolCalls()).peek(toolCalls -> {
            if (log.isDebugEnabled()) {
                for (AssistantMessage.ToolCall toolCall : toolCalls) {
                    log.debug("{} - payload: {}", toolCall.name(), toolCall.arguments());
                }
            }
        }).flatMap(List::stream).toList();
    }

    private ToolCallingChatOptions buildChatOption() {
        return ToolCallingChatOptions.builder().internalToolExecutionEnabled(false).build();
    }

    private Prompt builtInputPrompt(String content) {
        ToolCallingChatOptions chatOptions = buildChatOption();

        UserMessage userMessage = new UserMessage(content);
        Prompt prompt = new Prompt(List.of(buildSystemMessage(), userMessage), chatOptions);

        globalChatMemory.add(botOwnerId, prompt.getInstructions());
        return new Prompt(globalChatMemory.get(botOwnerId), buildChatOption());
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
