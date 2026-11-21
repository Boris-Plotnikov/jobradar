package dev.plotnikov.jobradar.vacancy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

public final class Fingerprints {
    public static final double SIMILAR_TITLE_THRESHOLD = 0.8;

    private Fingerprints() {}

    public static String normalizeTitle(String title) {
        if (title == null) {
            return "";
        }
        return title.toLowerCase(Locale.ROOT)
                .replaceAll("\\(?\\b[mwfdx]\\s*/\\s*[mwfdx](\\s*/\\s*[mwfdx])?\\b\\)?", " ")
                .replaceAll("\\(all genders\\)|\\(gn\\)|\\*in\\b|/in\\b", " ")
                .replaceAll("[^\\p{L}\\p{N}+#]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    public static String normalizeCompany(String company) {
        if (company == null) {
            return "";
        }
        return company.toLowerCase(Locale.ROOT)
                .replaceAll("\\b(gmbh|ag|se|ltd|inc|bv|b\\.v\\.|llc|kg|co)\\b", " ")
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    public static String fingerprint(String company, String title, String country) {
        String key = normalizeCompany(company) + "|" + normalizeTitle(title) + "|" + (country == null ? "" : country);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available in Java", e);
        }
    }

    public static double titleSimilarity(String a, String b) {
        Set<String> wordsA = new HashSet<>(Arrays.asList(normalizeTitle(a).split(" ")));
        Set<String> wordsB = new HashSet<>(Arrays.asList(normalizeTitle(b).split(" ")));
        Set<String> union = new HashSet<>(wordsA);
        union.addAll(wordsB);
        if (union.isEmpty()) {
            return 0;
        }
        Set<String> common = new HashSet<>(wordsA);
        common.retainAll(wordsB);
        return (double) common.size() / union.size();
    }
}
