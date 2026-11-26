package com.meetmind.ai;

public interface LlmProvider {

    record LlmResult(
            String provider,
            String model,
            String content,
            int tokensUsed
    ) {}

    LlmResult complete(String systemPrompt, String userPrompt);
}
