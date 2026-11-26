package com.meetmind.ai;

public interface EmbeddingProvider {
    float[] embed(String text);
    String providerName();
    String modelName();
}
