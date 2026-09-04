package dev.rainbow.aidevelop.workspace;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rainbow.ai.workspace")
public class WorkspaceProperties {
    private String root = "..";
    private int maxFiles = 20;
    private int maxFileChars = 3500;
    private int maxTotalChars = 16000;

    public String getRoot() { return root; }
    public void setRoot(String root) { this.root = root; }
    public int getMaxFiles() { return maxFiles; }
    public void setMaxFiles(int maxFiles) { this.maxFiles = maxFiles; }
    public int getMaxFileChars() { return maxFileChars; }
    public void setMaxFileChars(int maxFileChars) { this.maxFileChars = maxFileChars; }
    public int getMaxTotalChars() { return maxTotalChars; }
    public void setMaxTotalChars(int maxTotalChars) { this.maxTotalChars = maxTotalChars; }
}
