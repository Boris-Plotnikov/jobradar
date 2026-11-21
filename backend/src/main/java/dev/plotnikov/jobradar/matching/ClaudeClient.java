package dev.plotnikov.jobradar.matching;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.anthropic.models.messages.Usage;
import dev.plotnikov.jobradar.config.JobRadarProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ClaudeClient {
    private static final Logger log = LoggerFactory.getLogger(ClaudeClient.class);

    private final JobRadarProperties.Ai config;
    private final AiUsageRepository usageRepository;
    private final Clock clock;
    private AnthropicClient client;

    public ClaudeClient(JobRadarProperties properties, AiUsageRepository usageRepository, Clock clock) {
        this.config = properties.ai();
        this.usageRepository = usageRepository;
        this.clock = clock;
    }

    private synchronized AnthropicClient client() {
        if (client == null) {
            client = AnthropicOkHttpClient.builder()
                    .apiKey(config.apiKey())
                    .timeout(Duration.ofMinutes(3))
                    .maxRetries(2)
                    .build();
        }
        return client;
    }

    public <T> T structured(String model, String systemPrompt, String userPrompt, Class<T> type, String purpose,
                            Long vacancyId) {
        StructuredMessageCreateParams<T> params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(2000L)
                .systemOfTextBlockParams(cachedSystem(systemPrompt))
                .addUserMessage(userPrompt)
                .outputConfig(type)
                .build();
        Instant start = Instant.now(clock);
        StructuredMessage<T> response = client().messages().create(params);
        record(model, response.usage(), purpose, vacancyId, start);
        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(typed -> typed.text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("AI returned no structured text"));
    }

    public String text(String model, String systemPrompt, String userPrompt, String purpose, Long vacancyId) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(16000L)
                .thinking(ThinkingConfigAdaptive.builder().build())
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.HIGH).build())
                .systemOfTextBlockParams(cachedSystem(systemPrompt))
                .addUserMessage(userPrompt)
                .build();
        Instant start = Instant.now(clock);
        Message response = client().messages().create(params);
        record(model, response.usage(), purpose, vacancyId, start);
        StopReason stop = response.stopReason().orElse(null);
        if (StopReason.REFUSAL.equals(stop) || StopReason.MAX_TOKENS.equals(stop)) {
            throw new IllegalStateException("AI stopped with " + stop);
        }
        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .collect(Collectors.joining("\n"))
                .trim();
    }

    private static List<TextBlockParam> cachedSystem(String systemPrompt) {
        return List.of(TextBlockParam.builder()
                .text(systemPrompt)
                .cacheControl(CacheControlEphemeral.builder().build())
                .build());
    }

    private void record(String model, Usage usage, String purpose, Long vacancyId, Instant start) {
        long input = usage.inputTokens();
        long output = usage.outputTokens();
        long cacheRead = usage.cacheReadInputTokens().orElse(0L);
        long cacheWrite = usage.cacheCreationInputTokens().orElse(0L);
        AiUsage row = new AiUsage();
        row.setCreatedAt(Instant.now(clock));
        row.setPurpose(purpose);
        row.setModel(model);
        row.setInputTokens((int) input);
        row.setOutputTokens((int) output);
        row.setCacheReadTokens((int) cacheRead);
        row.setCacheWriteTokens((int) cacheWrite);
        row.setCostUsd(AiPricing.cost(model, input, output, cacheRead, cacheWrite));
        row.setVacancyId(vacancyId);
        row.setDurationMs((int) Duration.between(start, row.getCreatedAt()).toMillis());
        usageRepository.save(row);
        log.info("AI {} {}: in={} out={} cacheRead={} cost=${} {}ms", purpose, model, input, output, cacheRead,
                row.getCostUsd(), row.getDurationMs());
    }
}
