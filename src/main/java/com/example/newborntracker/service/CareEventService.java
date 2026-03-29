package com.example.newborntracker.service;

import com.example.newborntracker.model.CareEvent;
import com.example.newborntracker.model.EventType;
import com.example.newborntracker.model.RecordRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class CareEventService {

    private final AtomicLong idCounter = new AtomicLong(1);
    private final CopyOnWriteArrayList<CareEvent> events = new CopyOnWriteArrayList<>();

    public CareEvent record(RecordRequest request) {
        validateByType(request);
        CareEvent event = new CareEvent(
                idCounter.getAndIncrement(),
                request.type(),
                Instant.now(),
                request.amountMl(),
                request.durationMinutes()
        );
        events.add(event);
        return event;
    }

    public List<CareEvent> all() {
        ArrayList<CareEvent> copy = new ArrayList<>(events);
        copy.sort(Comparator.comparing(CareEvent::happenedAt).reversed());
        return copy;
    }

    private void validateByType(RecordRequest request) {
        EventType type = request.type();

        if (type == EventType.FORMULA && request.amountMl() == null) {
            throw new IllegalArgumentException("奶粉需要填写毫升数");
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
