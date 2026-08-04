package com.architectai.backend.ai.llm;

import com.architectai.backend.ai.AIProvider;

public interface LlmClient {
    String askForJson(AIProvider provider, String systemPrompt, String userPrompt);
}

