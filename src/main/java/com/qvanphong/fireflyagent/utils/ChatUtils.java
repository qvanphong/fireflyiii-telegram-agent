package com.qvanphong.fireflyagent.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


@Slf4j
@RequiredArgsConstructor
public class ChatUtils {
    private final DefaultAbsSender defaultAbsSender;

    public Integer sendProcessing(Long chatId) {
        return sendMessage(chatId, "🤔 Hmmm....");
    }

    public Integer sendMessage(Long chatId, String message) {
        SendMessage responseMessage = new SendMessage();
        responseMessage.setChatId(chatId.toString());
        responseMessage.setText(message);

        try {
            Message executed = defaultAbsSender.execute(responseMessage);
            return executed.getMessageId();
        } catch (TelegramApiException e) {
            log.error("Failed to reply Processing to user", e);
        }
        return null;
    }

    public void updateMessage(Long chatId, Integer msgId, String message) {
        EditMessageText editMessageText = EditMessageText.builder()
                .chatId(chatId.toString())
                .messageId(msgId)
                .text(message)
                .build();
        try {
            defaultAbsSender.execute(editMessageText);
            log.info("Reply sent successfully to {}", chatId);
        } catch (TelegramApiException e) {
            log.error("Error sending message", e);
        }
    }
}
