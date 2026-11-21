package dev.plotnikov.jobradar.notify;

import dev.plotnikov.jobradar.config.JobRadarProperties;
import dev.plotnikov.jobradar.pipeline.PipelineRun;
import dev.plotnikov.jobradar.vacancy.Vacancy;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

@Component
public class TelegramNotifier {
    private static final Logger log = LoggerFactory.getLogger(TelegramNotifier.class);

    private final RestClient http;
    private final JobRadarProperties properties;

    public TelegramNotifier(RestClient http, JobRadarProperties properties) {
        this.http = http;
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.telegram() != null && properties.telegram().enabled();
    }

    public boolean sendDigest(List<Vacancy> top, PipelineRun run) {
        if (!isEnabled()) {
            return false;
        }
        try {
            http.post()
                    .uri("https://api.telegram.org/bot{token}/sendMessage", properties.telegram().botToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "chat_id", properties.telegram().chatId(),
                            "text", formatDigest(top, run, properties.publicUrl()),
                            "parse_mode", "HTML",
                            "disable_web_page_preview", true))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RuntimeException e) {
            log.warn("Telegram digest failed: {}", e.getMessage());
            return false;
        }
    }

    public static String formatDigest(List<Vacancy> top, PipelineRun run, String publicUrl) {
        StringBuilder text = new StringBuilder();
        text.append("<b>JobRadar: ").append(top.size()).append(" new matches</b>\n");
        text.append("Checked ").append(run.getFetched()).append(" jobs, ")
                .append(run.getNewVacancies()).append(" new.\n\n");
        for (Vacancy v : top) {
            text.append("<b>").append(v.getMatchScore()).append("%</b> ")
                    .append("<a href=\"").append(HtmlUtils.htmlEscape(v.getUrl())).append("\">")
                    .append(HtmlUtils.htmlEscape(v.getTitle())).append("</a>\n")
                    .append(HtmlUtils.htmlEscape(v.getCompany()))
                    .append(" · ").append(v.getCountry() == null ? "?" : v.getCountry())
                    .append(" · ").append(v.getWorkMode().name().toLowerCase())
                    .append(" · via ").append(v.getSource()).append("\n");
            if (v.getMatchSummary() != null) {
                text.append("<i>").append(HtmlUtils.htmlEscape(v.getMatchSummary())).append("</i>\n");
            }
            text.append("<a href=\"").append(publicUrl).append("/#/vacancy/").append(v.getId())
                    .append("\">Open in JobRadar</a>\n\n");
        }
        return text.toString();
    }
}
