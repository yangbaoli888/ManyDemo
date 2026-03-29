package com.example.newborntracker.controller;

import com.example.newborntracker.model.CareEvent;
import com.example.newborntracker.model.RecordRequest;
import com.example.newborntracker.service.CareEventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class CareEventController {

    private final CareEventService service;

    public CareEventController(CareEventService service) {
        this.service = service;
    }

    @PostMapping
    public CareEvent record(@Valid @RequestBody RecordRequest request) {
        return service.record(request);
    }

    @GetMapping
    public List<CareEvent> list() {
        return service.all();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException ex) {
        return Map.of("message", ex.getMessage());
    }
}
