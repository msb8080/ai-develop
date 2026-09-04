package dev.rainbow.aidevelop.sandbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "rainbow.ai.sandbox")
public class SandboxProperties {
    private boolean enabled;
    private String dockerBinary = "docker";
    private String image = "maven:3.9-eclipse-temurin-17";
    private String mavenRepository;
    private Duration timeout = Duration.ofMinutes(3);
    private String memory = "768m";
    private String cpus = "1";
    private int pidsLimit = 128;
    private int maxOutputCharacters = 12000;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDockerBinary() { return dockerBinary; }
    public void setDockerBinary(String dockerBinary) { this.dockerBinary = dockerBinary; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getMavenRepository() { return mavenRepository; }
    public void setMavenRepository(String mavenRepository) { this.mavenRepository = mavenRepository; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
    public String getMemory() { return memory; }
    public void setMemory(String memory) { this.memory = memory; }
    public String getCpus() { return cpus; }
    public void setCpus(String cpus) { this.cpus = cpus; }
    public int getPidsLimit() { return pidsLimit; }
    public void setPidsLimit(int pidsLimit) { this.pidsLimit = pidsLimit; }
    public int getMaxOutputCharacters() { return maxOutputCharacters; }
    public void setMaxOutputCharacters(int maxOutputCharacters) { this.maxOutputCharacters = maxOutputCharacters; }
}
