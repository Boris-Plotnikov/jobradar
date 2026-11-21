package dev.plotnikov.jobradar.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jobradar")
public record JobRadarProperties(
        Owner owner,
        String timezone,
        Pipeline pipeline,
        Ai ai,
        Telegram telegram,
        String publicUrl) {
    public record Owner(String username, String password) {}

    public record Pipeline(String cron, boolean enabled, int maxAiPerRun) {}

    public record Ai(String apiKey, String matchModel, String letterModel, BigDecimal monthlyBudgetUsd) {
        public boolean enabled() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    public record Telegram(String botToken, String chatId) {
        public boolean enabled() {
            return botToken != null && !botToken.isBlank() && chatId != null && !chatId.isBlank();
        }
    }
}
