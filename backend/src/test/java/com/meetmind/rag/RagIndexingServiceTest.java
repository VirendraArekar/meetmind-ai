package com.meetmind.rag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RagIndexingServiceTest {

    @Test
    void cosineSimilarityReturnsOneForIdenticalVectorsAndZeroForOrthogonal() {
        float[] a = {1.0f, 0.0f, 0.0f};
        float[] b = {1.0f, 0.0f, 0.0f};
        float[] c = {0.0f, 1.0f, 0.0f};

        assertThat(RagIndexingService.cosineSimilarity(a, b)).isGreaterThan(0.999);
        assertThat(RagIndexingService.cosineSimilarity(a, c)).isEqualTo(0.0);
    }
}
