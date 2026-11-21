package dev.plotnikov.jobradar.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
@Validated
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    public record CreateRequest(@NotNull Long vacancyId) {}

    public record UpdateRequest(ApplicationStatus status, @Size(max = 10000) String notes) {}

    @GetMapping
    public List<ApplicationDto> board() {
        return service.board();
    }

    @PostMapping
    public ApplicationDto save(@RequestBody @jakarta.validation.Valid CreateRequest request) {
        return service.save(request.vacancyId());
    }

    @PatchMapping("/{id}")
    public ApplicationDto update(@PathVariable Long id, @RequestBody @jakarta.validation.Valid UpdateRequest request) {
        ApplicationDto result = null;
        if (request.status() != null) {
            result = service.move(id, request.status());
        }
        if (request.notes() != null) {
            result = service.updateNotes(id, request.notes());
        }
        if (result == null) {
            throw new IllegalArgumentException("Nothing to update: send status or notes");
        }
        return result;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
