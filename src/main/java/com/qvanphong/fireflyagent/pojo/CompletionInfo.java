package com.qvanphong.fireflyagent.pojo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
public class CompletionInfo {
    private List<AssistantMessage.ToolCall> toolsCalled;
    private String responseMessage;
    private String error;
    private boolean isSuccess = true;

    public void addToolCalled(Collection<AssistantMessage.ToolCall> tools) {
        if (toolsCalled == null) {
            toolsCalled = new ArrayList<>();
        }
        toolsCalled.addAll(tools);
    }

    public boolean hasToolCalled() {
        return toolsCalled != null;
    }
}
