package com.qvanphong.fireflyagent.pojo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CompletionInfo {
    @Getter
    private List<AssistantMessage.ToolCall> toolsCalled;

    @Getter @Setter
    private String responseMessage;


    public void addToolCalled(Collection<AssistantMessage.ToolCall> tools) {
        if (toolsCalled == null) {
            toolsCalled = new ArrayList<>();
        }
        toolsCalled.addAll(tools);

    }

    public boolean hasToolCalled() {
        return toolsCalled != null;
    }

    public List<AssistantMessage.ToolCall> getToolsCalled() {
        return toolsCalled;
    }
}
