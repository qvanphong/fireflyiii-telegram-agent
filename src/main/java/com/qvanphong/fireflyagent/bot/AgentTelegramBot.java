package com.qvanphong.fireflyagent.bot;

import com.qvanphong.fireflyagent.chat.LlmChatClient;
import com.qvanphong.fireflyagent.pojo.CompletionInfo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            String userName = update.getMessage().getFrom().getFirstName();

            log.info("Received message from {}: {}", userName, messageText);
            sendProcessing(chatId);

            CompletionInfo completion = llmChatClient.chat(messageText);
            StringBuilder responseBuilder = new StringBuilder();
            if (completion.hasToolCalled()) {
                responseBuilder.append("⚙️ Tools called: \n");
                for (String toolCall : completion.getToolsCalled()) {
                    responseBuilder.append("- ").append(toolCall).append("\n");
                }
            }
            responseBuilder.append("🤖: ").append(completion.getResponseMessage());

            SendMessage responseMessage = new SendMessage();
            responseMessage.setChatId(chatId.toString());
            responseMessage.setText(responseBuilder.toString());

            try {
                execute(responseMessage);
                log.info("Reply sent successfully to {}", userName);
            } catch (TelegramApiException e) {
                log.error("Error sending message", e);
            }
        }
    }

    private void sendProcessing(Long chatId) {
        SendMessage responseMessage = new SendMessage();
        responseMessage.setChatId(chatId.toString());
        responseMessage.setText("👍 Processing");

        try {
            execute(responseMessage);
        } catch (TelegramApiException e) {
            log.error("Failed to reply Processing to user", e);
        }
    }
}