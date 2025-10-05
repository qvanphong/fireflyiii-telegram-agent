package com.qvanphong.fireflyagent.pojo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CompletionInfo {
    @Getter
    private List<String> toolsCalled;

    @Getter @Setter
    private String responseMessage;

    public void addToolCall(String toolName) {
        if (toolsCalled == null) {
            toolsCalled = new ArrayList<>();
        }

        toolsCalled.add(toolName);
    }

    public void addToolCall(Collection<AssistantMessage.ToolCall> tools) {
        if (toolsCalled == null) {
            toolsCalled = new ArrayList<>();
        }

        tools.forEach(toolCall -> toolsCalled.add(toolCall.name()));

    }

    public boolean hasToolCalled() {
        return toolsCalled != null;
    }
}
