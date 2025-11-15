package com.qvanphong.fireflyagent.bot;

import com.qvanphong.fireflyagent.chat.LlmChatClient;
import com.qvanphong.fireflyagent.pojo.CompletionInfo;
import com.qvanphong.fireflyagent.utils.ChatUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentTelegramBot extends TelegramLongPollingBot {
    private final LlmChatClient llmChatClient;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.ownerId}")
    private Long ownerId;

    private ChatUtils chatUtils;

    @PostConstruct
    public void init() {
        chatUtils = new ChatUtils(this);
        log.info("Telegram bot initialized: {}", botUsername);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (isNotOwner(update) || hasNoMessage(update)) {
            return;
        }

        String messageText = update.getMessage().getText();
        if ("/reset".equals(messageText)) {
            resetLlmMemory(update.getMessage());
        } else {
            processMessageToLlm(update, messageText);
        }
    }


    private void resetLlmMemory(Message requestMessage) {
        Long chatUserId = requestMessage.getFrom().getId();
        llmChatClient.resetMemory(chatUserId.toString());
        chatUtils.sendMessage(requestMessage.getChatId(), "Memory reset");
    }

    private void processMessageToLlm(Update update, String messageText) {
        Long chatId = update.getMessage().getChatId();
        String userName = update.getMessage().getFrom().getFirstName();

        log.info("Received message from {}: {}", userName, messageText);
        Integer msgId = chatUtils.sendProcessing(chatId);

        CompletionInfo completion = llmChatClient.completion(messageText);
        String responseText = !completion.isSuccess() ?
                buildFailResponseMessage(completion) :
                buildResponseMessage(completion);
        chatUtils.updateMessage(chatId, msgId, responseText);
    }

    private String buildResponseMessage(CompletionInfo completion) {
        StringBuilder responseBuilder = new StringBuilder();

        responseBuilder.append("🤖: ").append(completion.getResponseMessage());
        responseBuilder.append("\n\n");
        if (completion.hasToolCalled()) {
            responseBuilder.append("⚙️ Tools called: \n");
            for (AssistantMessage.ToolCall toolCall : completion.getToolsCalled()) {
                responseBuilder.append("- ").append(toolCall.name()).append("\n");
            }
        }
        return responseBuilder.toString();
    }

    private String buildFailResponseMessage(CompletionInfo completionInfo) {
        return "❌ Failed to process, please try again. \nError: %s".formatted(completionInfo.getError());
    }

    private boolean hasNoMessage(Update update) {
        return !update.hasMessage() || !update.getMessage().hasText();
    }

    private boolean isNotOwner(Update update) {
        return update.hasMessage() && !update.getMessage().getFrom().getId().equals(ownerId);
    }
}