package dev.sunbirdrc.claim.service;

import dev.sunbirdrc.claim.entity.Event;
import dev.sunbirdrc.claim.entity.TelemetryObject;
import dev.sunbirdrc.claim.repository.EventRepository;
import dev.sunbirdrc.claim.repository.TelemetryObjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    public Event saveEventWithTelemetry(Event event) {

        Event savedEvent = eventRepository.save(event);

        return savedEvent;
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }
}

