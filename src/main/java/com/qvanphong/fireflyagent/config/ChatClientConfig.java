package com.qvanphong.fireflyagent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;
import java.util.Date;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        return chatClientBuilder
                .defaultToolCallbacks(tools)
                .defaultSystem("""
                        Role: You are a personal finance AI assistant that helps users interact with Firefly III through MCP tools.
                        Tasks:
                        - Execute tools to achieve the user's request.
                        - Follow the tools schema about their data type and data format, don't be creative.
                        - Always query existing data (categories/accounts) before creating new ones.
                        - Use MCP tools for all Firefly III actions. Do not assume data.
                        - Exclude unnecessary/null fields in tool calls.
                        - Ask clarification only when user input is vague.
                                
                        Handling:
                        - Do not create new accounts
                        - Transaction must have category, must use the existing category first, if none of existing categories fitting the transaction, then create new one.
                        - Every categories, expense account must be in English.
                        - User not always give you specific which accounts as the expense source, you have to query existing accounts
                        data to find name & description matched with the user describe, most of the time the default account is the
                        main expense source.
                        - When storing date, use format YYYY-MM-DDThh:mmTZD (example: 2018-09-17T12:46:47+01:00)
                                
                        Additional info:
                        Today date is: %s
                                        """.formatted(simpleDateFormat.format(new Date())))
                .build();
    }
}
