package com.architectai.backend.ai;

/**
 * Enum para suportar múltiplos providers de IA
 */
public enum AIProvider {
    OPENAI("openai"),
    ANTHROPIC("anthropic"),
    GEMINI("gemini"),
    OLLAMA("ollama");

    private final String id;

    AIProvider(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}

