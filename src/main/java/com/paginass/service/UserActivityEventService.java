package com.paginass.service;

import com.paginass.entity.UserEvent;
import com.paginass.repository.UserEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserActivityEventService {

    private final UserEventRepository userEventRepository;

    @Transactional
    public void record(UUID userId, String eventType, String entityType, UUID entityId, Map<String, Object> metadata) {
        UserEvent event = new UserEvent();
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setMetadata(metadata);
        event.setOccurredAt(OffsetDateTime.now(ZoneOffset.UTC));
        userEventRepository.save(event);
    }
}
