package dev.plotnikov.jobradar.vacancy;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CountryResolver {
    public record Place(String country, String city) {}

    private static final Map<String, String> COUNTRY_WORDS = new LinkedHashMap<>();
    private static final Map<String, String> CITY_WORDS = new LinkedHashMap<>();

    static {
        country("AT", "austria", "österreich", "oesterreich");
        country("DE", "germany", "deutschland");
        country("CH", "switzerland", "schweiz", "suisse");
        country("NL", "netherlands", "nederland", "holland");
        country("GB", "united kingdom", "england", "scotland", " uk");
        country("FR", "france");
        country("ES", "spain", "españa");
        country("PT", "portugal");
        country("PL", "poland", "polska");
        country("CZ", "czech", "czechia");
        country("IE", "ireland");
        country("BE", "belgium");
        country("SE", "sweden");
        country("DK", "denmark");
        country("IT", "italy", "italia");
        country("US", "united states", "usa", "u.s.");
        country("CA", "canada");
        country("EU", "europe", "emea", "european union", "cet timezone");
        country("WW", "worldwide", "anywhere", "global");

        city("AT", "vienna", "wien", "graz", "linz", "salzburg", "innsbruck", "klagenfurt");
        city("DE", "berlin", "munich", "münchen", "muenchen", "hamburg", "frankfurt", "cologne", "köln", "stuttgart",
                "düsseldorf", "dusseldorf", "hanover", "hannover", "leipzig", "dresden", "nuremberg", "nürnberg",
                "bremen", "essen", "dortmund", "karlsruhe", "mannheim", "bonn");
        city("CH", "zurich", "zürich", "geneva", "genf", "basel", "bern", "lausanne", "zug");
        city("NL", "amsterdam", "rotterdam", "utrecht", "eindhoven", "the hague", "den haag");
        city("GB", "london", "manchester", "edinburgh", "cambridge", "bristol");
        city("FR", "paris", "lyon", "toulouse");
        city("ES", "madrid", "barcelona", "valencia");
        city("PT", "lisbon", "lisboa", "porto");
        city("PL", "warsaw", "krakow", "kraków", "wroclaw");
        city("CZ", "prague", "praha", "brno");
        city("IE", "dublin");
        city("BE", "brussels");
        city("SE", "stockholm");
        city("DK", "copenhagen");
        city("IT", "milan", "milano", "rome");
    }

    private static void country(String code, String... words) {
        for (String word : words) {
            COUNTRY_WORDS.put(word, code);
        }
    }

    private static void city(String code, String... words) {
        for (String word : words) {
            CITY_WORDS.put(word, code);
        }
    }

    public Place resolve(String location, String countryHint) {
        String text = location == null ? "" : " " + location.toLowerCase(Locale.ROOT) + " ";
        String city = null;
        String country = null;

        for (Map.Entry<String, String> entry : CITY_WORDS.entrySet()) {
            if (text.contains(entry.getKey())) {
                city = capitalize(entry.getKey());
                country = entry.getValue();
                break;
            }
        }
        if (country == null) {
            for (Map.Entry<String, String> entry : COUNTRY_WORDS.entrySet()) {
                if (text.contains(entry.getKey())) {
                    country = entry.getValue();
                    break;
                }
            }
        }
        if (country == null && countryHint != null && !countryHint.isBlank()) {
            country = countryHint.toUpperCase(Locale.ROOT);
        }
        return new Place(country, city);
    }

    private static String capitalize(String word) {
        return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
    }
}
