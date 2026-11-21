package dev.plotnikov.jobradar.matching;

import dev.plotnikov.jobradar.settings.Settings;
import dev.plotnikov.jobradar.vacancy.Vacancy;

public interface MatchScorer {
    MatchResult score(Vacancy vacancy, Settings settings);

    String method();
}
