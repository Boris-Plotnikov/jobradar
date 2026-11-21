package dev.plotnikov.jobradar.matching;

import dev.plotnikov.jobradar.settings.Settings;
import dev.plotnikov.jobradar.vacancy.Vacancy;
import dev.plotnikov.jobradar.vacancy.WorkMode;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class Prefilter {
    private static final Pattern YEARS = Pattern.compile(
            "(\\d{1,2})\\s*\\+?\\s*(years|yrs|jahre)", Pattern.CASE_INSENSITIVE);
    static final int MAX_YEARS = 3;

    public PrefilterResult check(Vacancy v, Settings settings) {
        String title = v.getTitle().toLowerCase(Locale.ROOT);

        for (String stop : settings.stopWordList()) {
            if (containsWord(title, stop)) {
                return PrefilterResult.reject("Title contains stop word '" + stop + "'");
            }
        }

        Integer years = requiredYears(v.getDescription());
        if (years != null && years > MAX_YEARS) {
            return PrefilterResult.reject("Requires " + years + "+ years of experience");
        }

        if (settings.isRemoteOnly() && v.getWorkMode() != WorkMode.REMOTE) {
            return PrefilterResult.reject("Not remote");
        }

        if (!countryAllowed(v, settings.countryList())) {
            return PrefilterResult.reject("Country " + (v.getCountry() == null ? "unknown" : v.getCountry())
                    + " is not in the search list");
        }

        String searchable = (title + " " + v.getSkills() + " "
                + head(v.getDescription(), 1500)).toLowerCase(Locale.ROOT);
        boolean keywordFound = settings.keywordList().stream().anyMatch(k -> containsWord(searchable, k));
        if (!keywordFound) {
            return PrefilterResult.reject("No search keyword found");
        }

        return PrefilterResult.pass(bestDirection(title, settings.directionList()));
    }

    static boolean containsWord(String text, String word) {
        if (word.isBlank()) {
            return false;
        }
        Pattern p = Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(word) + "(?![\\p{L}\\p{N}])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        return p.matcher(text).find();
    }

    static Integer requiredYears(String description) {
        if (description == null) {
            return null;
        }
        Matcher m = YEARS.matcher(head(description, 4000));
        Integer max = null;
        while (m.find()) {
            int years = Integer.parseInt(m.group(1));
            if (years <= 20 && (max == null || years > max)) {
                max = years;
            }
        }
        return max;
    }

    static boolean countryAllowed(Vacancy v, List<String> allowed) {
        String country = v.getCountry();
        if (country != null && allowed.contains(country)) {
            return true;
        }
        boolean euRemote = allowed.contains("EU") && v.getWorkMode() == WorkMode.REMOTE
                && (country == null || country.equals("EU") || country.equals("WW"));
        return euRemote;
    }

    static String bestDirection(String title, List<String> directions) {
        String best = null;
        int bestHits = 0;
        for (String direction : directions) {
            int hits = 0;
            for (String word : direction.toLowerCase(Locale.ROOT).split("\\s+")) {
                if (word.length() > 2 && containsWord(title, word)) {
                    hits++;
                }
            }
            if (hits > bestHits) {
                bestHits = hits;
                best = direction;
            }
        }
        return best != null ? best : "Other";
    }

    private static String head(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() > max ? text.substring(0, max) : text;
    }
}
