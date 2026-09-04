package dev.rainbow.aidevelop.context;

import java.util.List;

public record AssembledContext(String systemPrompt, String userPrompt, List<String> sources, boolean truncated) {
}
