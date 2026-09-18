package com.uiu.examintegrity.proctoring.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.uiu.examintegrity.proctoring.api.dto.EventDto;
import com.uiu.examintegrity.proctoring.application.EventIngestionService;
import com.uiu.examintegrity.user.AppUserDetails;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@RestController
public class EventController {
    private final EventIngestionService ingestionService;

    public EventController(EventIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/api/sessions/{id}/events")
    public ResponseEntity<Void> ingest(@PathVariable Long id,
            @RequestBody @Size(max = 500, message = "At most 500 events per batch") List<@Valid EventDto> batch,
            @AuthenticationPrincipal AppUserDetails student) {
        ingestionService.accept(id, student.getId(), batch.stream().map(EventDto::toIncoming).toList());
        return ResponseEntity.accepted().build();
    }
}
