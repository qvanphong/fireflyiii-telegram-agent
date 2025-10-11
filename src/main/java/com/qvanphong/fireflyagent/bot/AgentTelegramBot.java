package com.qvanphong.fireflyagent.bot;

import com.qvanphong.fireflyagent.chat.LlmChatClient;
import com.qvanphong.fireflyagent.pojo.CompletionInfo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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

    @PostConstruct
    public void init() {
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
        if (update.hasMessage() && !update.getMessage().getFrom().getId().equals(ownerId)) {
            return;
        }
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String messageText = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        String userName = update.getMessage().getFrom().getFirstName();

        log.info("Received message from {}: {}", userName, messageText);
        Integer msgId = sendProcessing(chatId);

        CompletionInfo completion = llmChatClient.completion(messageText);
        StringBuilder responseBuilder = new StringBuilder();

        responseBuilder.append("🤖: ").append(completion.getResponseMessage());
        responseBuilder.append("\n\n");
        if (completion.hasToolCalled()) {
            responseBuilder.append("⚙️ Tools called: \n");
            for (AssistantMessage.ToolCall toolCall : completion.getToolsCalled()) {
                responseBuilder.append("- ").append(toolCall.name()).append("\n");
            }
        }

        EditMessageText editMessageText = EditMessageText.builder()
                .chatId(chatId.toString())
                .messageId(msgId)
                .text(responseBuilder.toString())
                .build();

        try {
            execute(editMessageText);
            log.info("Reply sent successfully to {}", userName);
        } catch (TelegramApiException e) {
            log.error("Error sending message", e);
        }
    }

    private Integer sendProcessing(Long chatId) {
        SendMessage responseMessage = new SendMessage();
        responseMessage.setChatId(chatId.toString());
        responseMessage.setText("🤔 Hmmm....");

        try {
            Message executed = execute(responseMessage);
            return executed.getMessageId();
        } catch (TelegramApiException e) {
            log.error("Failed to reply Processing to user", e);
        }
        return null;
    }
}