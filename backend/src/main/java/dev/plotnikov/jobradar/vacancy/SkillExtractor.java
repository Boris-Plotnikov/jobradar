package dev.plotnikov.jobradar.vacancy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SkillExtractor {
    private static final Map<String, Pattern> SKILLS = new LinkedHashMap<>();

    static {
        add("Java", "\\bjava\\b(?!\\s*script)");
        add("Spring", "\\bspring(\\s*boot)?\\b");
        add("Kotlin", "\\bkotlin\\b");
        add("C++", "(?<![\\w+])c\\+\\+");
        add("C#", "(?<![\\w#])c#");
        add(".NET", "\\.net\\b");
        add("Python", "\\bpython\\b");
        add("JavaScript", "\\bjavascript\\b|\\bjs\\b");
        add("TypeScript", "\\btypescript\\b");
        add("React", "\\breact(\\.js)?\\b");
        add("Angular", "\\bangular\\b");
        add("Vue", "\\bvue(\\.js)?\\b");
        add("Node.js", "\\bnode(\\.js|js)?\\b");
        add("Go", "\\bgolang\\b|\\bgo\\s+developer\\b");
        add("Rust", "\\brust\\b");
        add("PHP", "\\bphp\\b");
        add("Scala", "\\bscala\\b");
        add("SQL", "\\bsql\\b");
        add("PostgreSQL", "\\bpostgres(ql)?\\b");
        add("MySQL", "\\bmysql\\b");
        add("MongoDB", "\\bmongo(db)?\\b");
        add("Redis", "\\bredis\\b");
        add("Kafka", "\\bkafka\\b");
        add("Hibernate/JPA", "\\bhibernate\\b|\\bjpa\\b");
        add("REST API", "\\brest(ful)?\\b|\\brest\\s*api");
        add("Microservices", "\\bmicro-?services?\\b");
        add("Docker", "\\bdocker\\b");
        add("Kubernetes", "\\bkubernetes\\b|\\bk8s\\b");
        add("AWS", "\\baws\\b|amazon web services");
        add("Azure", "\\bazure\\b");
        add("GCP", "\\bgcp\\b|google cloud");
        add("Terraform", "\\bterraform\\b");
        add("CI/CD", "\\bci\\s*/\\s*cd\\b|continuous integration");
        add("Git", "\\bgit\\b|\\bgithub\\b|\\bgitlab\\b");
        add("Linux", "\\blinux\\b");
        add("JUnit", "\\bjunit\\b");
        add("Maven/Gradle", "\\bmaven\\b|\\bgradle\\b");
        add("Agile/Scrum", "\\bagile\\b|\\bscrum\\b");
        add("Machine Learning", "\\bmachine learning\\b|\\bml\\b");
        add("German", "\\bgerman\\b|\\bdeutsch\\b|deutschkenntnisse");
    }

    private static void add(String name, String regex) {
        SKILLS.put(name, Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
    }

    public List<String> extract(String text) {
        List<String> found = new ArrayList<>();
        if (text == null) {
            return found;
        }
        for (Map.Entry<String, Pattern> entry : SKILLS.entrySet()) {
            if (entry.getValue().matcher(text).find()) {
                found.add(entry.getKey());
            }
        }
        return found;
    }

    public static List<String> knownSkills() {
        return new ArrayList<>(SKILLS.keySet());
    }
}
