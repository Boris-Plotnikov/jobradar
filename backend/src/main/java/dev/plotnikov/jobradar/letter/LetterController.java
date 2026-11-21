package dev.plotnikov.jobradar.letter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LetterController {

    private final LetterService service;

    public LetterController(LetterService service) {
        this.service = service;
    }

    public record GenerateRequest(@NotBlank String language) {}

    public record SaveRequest(@NotBlank String language, @NotBlank @Size(max = 10000) String body) {}

    public record TemplateRequest(@NotBlank @Size(max = 10000) String body) {}

    @GetMapping("/vacancies/{id}/letters")
    public List<CoverLetter> versions(@PathVariable Long id) {
        return service.versions(id);
    }

    @PostMapping("/vacancies/{id}/letters/generate")
    public CoverLetter generate(@PathVariable Long id, @Valid @RequestBody GenerateRequest request) {
        return service.generate(id, request.language());
    }

    @PostMapping("/vacancies/{id}/letters")
    public CoverLetter save(@PathVariable Long id, @Valid @RequestBody SaveRequest request) {
        return service.saveManual(id, request.language(), request.body());
    }

    @GetMapping("/templates")
    public List<PromptTemplate> templates() {
        return service.templates();
    }

    @PutMapping("/templates/{id}")
    public PromptTemplate updateTemplate(@PathVariable Long id, @Valid @RequestBody TemplateRequest request) {
        return service.updateTemplate(id, request.body());
    }
}
