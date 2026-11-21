package dev.plotnikov.jobradar.vacancy;

import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class LanguageDetector {
    private static final Set<String> GERMAN = Set.of(
            "und", "der", "die", "das", "wir", "mit", "für", "sie", "ist", "eine", "unsere", "bei", "auf", "zu", "deine");
    private static final Set<String> ENGLISH = Set.of(
            "and", "the", "we", "with", "for", "you", "is", "our", "are", "to", "your", "of", "in", "will", "an");

    public String detect(String text) {
        if (text == null || text.isBlank()) {
            return "EN";
        }
        String sample = text.length() > 3000 ? text.substring(0, 3000) : text;
        int german = 0;
        int english = 0;
        for (String word : sample.toLowerCase(Locale.ROOT).split("[^\\p{L}]+")) {
            if (GERMAN.contains(word)) {
                german++;
            }
            if (ENGLISH.contains(word)) {
                english++;
            }
        }
        return german > english ? "DE" : "EN";
    }
}
