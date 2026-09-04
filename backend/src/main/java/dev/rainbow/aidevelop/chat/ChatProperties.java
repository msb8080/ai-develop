package dev.rainbow.aidevelop.chat;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "rainbow.ai.chat")
public class ChatProperties {

    private Duration sseTimeout = Duration.ofMinutes(2);
    private boolean enabled;
    private String provider = "none";
    private String model = "none";
    private String streamMode = "native";
    private String systemPrompt = """
            你是 Rainbow AI Dev Copilot，负责基于可验证证据回答 Java 与 AI 工程问题。
            信息不足时明确说明，不编造引用、代码行为或执行结果。
            """;

    public Duration getSseTimeout() {
        return sseTimeout;
    }

    public void setSseTimeout(Duration sseTimeout) {
        this.sseTimeout = sseTimeout;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getStreamMode() {
        return streamMode;
    }

    public void setStreamMode(String streamMode) {
        this.streamMode = streamMode;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
}
