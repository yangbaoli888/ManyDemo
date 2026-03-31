package com.example.newborntracker.controller;

import com.example.newborntracker.model.ActiveStatus;
import com.example.newborntracker.model.CareEvent;
import com.example.newborntracker.model.EndRecordRequest;
import com.example.newborntracker.model.QuickRecordRequest;
import com.example.newborntracker.model.StartRecordRequest;
import com.example.newborntracker.model.TimelineDay;
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

    @PostMapping("/quick")
    public CareEvent quickRecord(@Valid @RequestBody QuickRecordRequest request) {
        return service.quickRecord(request.type());
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void start(@Valid @RequestBody StartRecordRequest request) {
        service.start(request.type());
    }

    @PostMapping("/end")
    public CareEvent end(@Valid @RequestBody EndRecordRequest request) {
        return service.end(request.type(), request.amountMl());
    }

    @GetMapping("/status")
    public ActiveStatus status() {
        return service.status();
    }

    @GetMapping
    public List<CareEvent> list() {
        return service.all();
    }

    @GetMapping("/timeline")
    public List<TimelineDay> timeline() {
        return service.timelineByDate();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException ex) {
        return Map.of("message", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleIllegalState(IllegalStateException ex) {
        return Map.of("message", ex.getMessage());
    }
}
