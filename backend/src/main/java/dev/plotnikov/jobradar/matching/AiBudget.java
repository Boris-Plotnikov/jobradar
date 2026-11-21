package dev.plotnikov.jobradar.matching;

import dev.plotnikov.jobradar.config.JobRadarProperties;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;

@Service
public class AiBudget {
    private final AiUsageRepository usage;
    private final JobRadarProperties properties;
    private final Clock clock;

    public AiBudget(AiUsageRepository usage, JobRadarProperties properties, Clock clock) {
        this.usage = usage;
        this.properties = properties;
        this.clock = clock;
    }

    public BigDecimal spentThisMonth() {
        return usage.sumCostSince(startOfMonth());
    }

    public BigDecimal limit() {
        return properties.ai().monthlyBudgetUsd();
    }

    public boolean aiAvailable() {
        return properties.ai().enabled() && spentThisMonth().compareTo(limit()) < 0;
    }

    public String offReason() {
        if (!properties.ai().enabled()) {
            return "No ANTHROPIC_API_KEY configured";
        }
        if (spentThisMonth().compareTo(limit()) >= 0) {
            return "Monthly AI budget of $" + limit() + " is used up";
        }
        return null;
    }

    private Instant startOfMonth() {
        ZoneId zone = ZoneId.of(properties.timezone());
        return LocalDate.now(clock.withZone(zone)).withDayOfMonth(1).atStartOfDay(zone).toInstant();
    }
}
