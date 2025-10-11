package com.qvanphong.fireflyagent.config;

import com.qvanphong.fireflyagent.utils.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class ChatClientConfig {

    @Value("${spring.ai.prompt.systemPromptPath}")
    private String systemPromptPath;


    @Bean(name = "llmSystemPrompt")
    public String llmSystemPrompt() throws IOException {
        return FileUtils.readFileContent(StringUtils.defaultIfBlank(systemPromptPath, "classpath:default_prompt.txt"));
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder,
                                 ToolCallbackProvider tools,
                                 ChatMemory chatMemory) {

        return chatClientBuilder
                .defaultToolCallbacks(tools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

}
