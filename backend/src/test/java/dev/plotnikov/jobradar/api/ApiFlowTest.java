package dev.plotnikov.jobradar.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.plotnikov.jobradar.settings.SettingsService;
import dev.plotnikov.jobradar.vacancy.Vacancy;
import dev.plotnikov.jobradar.vacancy.VacancyRepository;
import dev.plotnikov.jobradar.vacancy.WorkMode;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ApiFlowTest {

    @Autowired MockMvc mvc;
    @Autowired VacancyRepository vacancies;
    @Autowired SettingsService settings;
    private Long vacancyId;

    @BeforeEach
    void createVacancy() {
        Vacancy v = new Vacancy();
        v.setSource("Test");
        v.setExternalId("x1");
        v.setUrl("https://example.com/x1");
        v.setTitle("Junior Java Developer");
        v.setCompany("Acme");
        v.setCountry("AT");
        v.setWorkMode(WorkMode.HYBRID);
        v.setLanguage("EN");
        v.setDescription("Java and Spring");
        v.setSkills("Java,Spring");
        v.setFetchedAt(Instant.now());
        v.setPublishedAt(Instant.now());
        v.setFingerprint("f".repeat(64));
        v.setPrefilterPassed(true);
        v.setMatchScore(85);
        v.setMatchSummary("Strong fit");
        v.setScoredBy("AI");
        vacancyId = vacancies.save(v).getId();
    }

    @Test
    void privateApiNeedsLogin() throws Exception {
        mvc.perform(get("/api/vacancies")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/settings")).andExpect(status().isUnauthorized());
    }

    @Test
    void showcaseIsPublicAndHidesPersonalData() throws Exception {
        mvc.perform(get("/api/public/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Junior Java Developer"))
                .andExpect(jsonPath("$[0].applicationStatus").doesNotExist())
                .andExpect(jsonPath("$[0].description").doesNotExist());
        mvc.perform(get("/api/public/summary")).andExpect(status().isOk())
                .andExpect(jsonPath("$.strongMatchesLast30Days").value(1));
    }

    @Test
    void aiTraceIsPublicAndHidesCv() throws Exception {
        String cv = settings.get().getResumeText();
        assertThat(cv).hasSizeGreaterThan(40);

        String body = mvc.perform(get("/api/public/ai-trace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vacancyId").value(vacancyId))
                .andExpect(jsonPath("$.answer.score").value(85))
                .andExpect(jsonPath("$.userPrompt", containsString("Junior Java Developer")))
                .andExpect(jsonPath("$.instructions", containsString("rate the fit")))
                .andExpect(jsonPath("$.cvCharacters").value(cv.length()))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain(cv.substring(0, 40));

        mvc.perform(get("/api/public/ai-trace").param("vacancyId", "999999")).andExpect(status().isNotFound());
    }

    @Test
    void loginWithWrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"boris\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"boris\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/api/applications").with(user("boris")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vacancyId\":" + vacancyId + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void applicationBoardFlow() throws Exception {
        String body = mvc.perform(post("/api/applications").with(user("boris")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"vacancyId\":" + vacancyId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SAVED"))
                .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(patch("/api/applications/" + id).with(user("boris")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPLIED\",\"notes\":\"Sent via site\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.appliedAt").exists())
                .andExpect(jsonPath("$.needsFollowUp").value(false));

        mvc.perform(get("/api/vacancies/" + vacancyId).with(user("boris")))
                .andExpect(jsonPath("$.applicationStatus").value("APPLIED"));
    }

    @Test
    void letterWithoutAiUsesTemplateAndKeepsVersions() throws Exception {
        mvc.perform(post("/api/vacancies/" + vacancyId + "/letters/generate").with(user("boris")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"language\":\"DE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedBy").value("TEMPLATE"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.body", containsString("Sehr geehrte")));
        mvc.perform(post("/api/vacancies/" + vacancyId + "/letters").with(user("boris")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"language\":\"DE\",\"body\":\"Edited\"}"))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.generatedBy").value("MANUAL"));
        mvc.perform(post("/api/vacancies/" + vacancyId + "/letters/generate").with(user("boris")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"language\":\"FR\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void settingsValidation() throws Exception {
        mvc.perform(put("/api/settings").with(user("boris")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resumeText\":\"\",\"directions\":\"x\",\"countries\":\"AT\",\"keywords\":\"java\","
                                + "\"stopWords\":\"\",\"minScore\":150,\"digestHour\":8,\"remoteOnly\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void feedFiltersAndInsights() throws Exception {
        mvc.perform(get("/api/vacancies").param("country", "DE").with(user("boris")))
                .andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(get("/api/vacancies").param("minScore", "80").with(user("boris")))
                .andExpect(jsonPath("$.content", hasSize(1)));
        mvc.perform(get("/api/insights").with(user("boris")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relevantVacancies").value(1))
                .andExpect(jsonPath("$.topSkillsRelevant[0].name").exists());
    }
}
