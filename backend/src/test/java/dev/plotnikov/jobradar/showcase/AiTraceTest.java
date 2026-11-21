package dev.plotnikov.jobradar.showcase;

import static org.assertj.core.api.Assertions.assertThat;

import dev.plotnikov.jobradar.matching.AiUsage;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiTraceTest {

    private static AiUsage call(int input, int output, int cacheRead, String cost) {
        AiUsage u = new AiUsage();
        u.setModel("claude-haiku-5-5");
        u.setInputTokens(input);
        u.setOutputTokens(output);
        u.setCacheReadTokens(cacheRead);
        u.setCacheWriteTokens(0);
        u.setCostUsd(new BigDecimal(cost));
        return u;
    }

    @Test
    void schemaComesFromMatchResultRecord() {
        List<AiTrace.SchemaField> schema = AiTraceService.schema();

        assertThat(schema).extracting(AiTrace.SchemaField::name)
                .containsExactly("score", "summary", "strengths", "gaps", "advice");
        assertThat(schema.get(0).type()).isEqualTo("int");
        assertThat(schema.get(2).type()).isEqualTo("List<String>");
        assertThat(schema.get(0).description()).contains("0 and 100");
    }

    @Test
    void cacheSavingIsComparedWithFullInputPrice() {
        AiUsage first = call(100, 50, 1000, "0.000045");
        AiUsage second = call(100, 50, 1000, "0.000045");

        AiTrace.UsageStats stats = AiTraceService.summarize(List.of(first, second), List.of(),
                new BigDecimal("0.09"), new BigDecimal("5"));

        assertThat(stats.scoringCalls()).isEqualTo(2);
        assertThat(stats.totalCostUsd()).isEqualByComparingTo("0.00009");
        assertThat(stats.costWithoutCacheUsd()).isEqualByComparingTo("0.00027");
        assertThat(stats.cacheSavingPercent()).isEqualTo(67);
        assertThat(stats.avgCacheReadTokens()).isEqualTo(1000);
        assertThat(stats.avgLetterCostUsd()).isEqualByComparingTo("0");
    }

    @Test
    void noCallsGiveZeros() {
        AiTrace.UsageStats stats = AiTraceService.summarize(List.of(), List.of(), BigDecimal.ZERO, BigDecimal.ONE);

        assertThat(stats.scoringCalls()).isZero();
        assertThat(stats.cacheSavingPercent()).isZero();
        assertThat(stats.avgCostUsd()).isEqualByComparingTo("0");
    }
}
