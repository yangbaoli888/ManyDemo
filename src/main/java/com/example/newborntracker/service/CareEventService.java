package com.example.newborntracker.service;

import com.example.newborntracker.model.CareEvent;
import com.example.newborntracker.model.EventType;
import com.example.newborntracker.model.RecordRequest;
import com.example.newborntracker.model.TimelineDay;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class CareEventService {

    private final AtomicLong idCounter = new AtomicLong(1);
    private final CopyOnWriteArrayList<CareEvent> events = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;
    private final Path storageFile;

    public CareEventService(ObjectMapper objectMapper,
                            @Value("${tracker.storage-file:data/events.json}") String storageFile) {
        this.objectMapper = objectMapper;
        this.storageFile = Paths.get(storageFile);
    }

    @PostConstruct
    void init() {
        loadFromDisk();
    }

    public synchronized CareEvent record(RecordRequest request) {
        validateByType(request);
        CareEvent event = new CareEvent(
                idCounter.getAndIncrement(),
                request.type(),
                Instant.now(),
                request.amountMl(),
                request.durationMinutes()
        );
        events.add(event);
        persist();
        return event;
    }

    public List<CareEvent> all() {
        ArrayList<CareEvent> copy = new ArrayList<>(events);
        copy.sort(Comparator.comparing(CareEvent::happenedAt).reversed());
        return copy;
    }

    public List<TimelineDay> timelineByDate() {
        List<CareEvent> sorted = all();
        Map<LocalDate, List<CareEvent>> grouped = new LinkedHashMap<>();

        for (CareEvent event : sorted) {
            LocalDate date = event.happenedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            grouped.computeIfAbsent(date, ignored -> new ArrayList<>()).add(event);
        }

        List<TimelineDay> timeline = new ArrayList<>();
        grouped.forEach((date, eventsInDay) -> timeline.add(new TimelineDay(date, eventsInDay)));
        return timeline;
    }

    private void loadFromDisk() {
        try {
            if (!Files.exists(storageFile)) {
                return;
            }
            List<CareEvent> stored = objectMapper.readValue(storageFile.toFile(), new TypeReference<>() {});
            events.clear();
            events.addAll(stored);
            long maxId = stored.stream().mapToLong(CareEvent::id).max().orElse(0L);
            idCounter.set(maxId + 1);
        } catch (IOException e) {
            throw new IllegalStateException("加载打卡记录失败", e);
        }
    }

    private void persist() {
        try {
            Path parent = storageFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storageFile.toFile(), events);
        } catch (IOException e) {
            throw new IllegalStateException("保存打卡记录失败", e);
        }
    }

    private void validateByType(RecordRequest request) {
        EventType type = request.type();

        if (type == EventType.FORMULA && request.amountMl() == null) {
            throw new IllegalArgumentException("奶粉需要填写毫升数");
        }
        if (type == EventType.FORMULA && request.amountMl() % 30 != 0) {
            throw new IllegalArgumentException("奶粉毫升数必须为30的倍数");
        }
        if (type == EventType.BREASTFEEDING && request.durationMinutes() == null) {
            throw new IllegalArgumentException("母乳需要填写时长（分钟）");
        }
        if ((type == EventType.STOOL || type == EventType.URINE)
                && (request.amountMl() != null || request.durationMinutes() != null)) {
            throw new IllegalArgumentException("大小便只记录时间，不需要填写量或时长");
        }
        if (type == EventType.FORMULA && request.durationMinutes() != null) {
            throw new IllegalArgumentException("奶粉记录不需要填写时长");
        }
        if (type == EventType.BREASTFEEDING && request.amountMl() != null) {
            throw new IllegalArgumentException("母乳记录不需要填写毫升数");
        }
    }
}
