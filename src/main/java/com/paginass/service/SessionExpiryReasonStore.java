package com.paginass.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionExpiryReasonStore {

    private final ConcurrentHashMap<String, String> reasons = new ConcurrentHashMap<>();

    public void register(String sessionId, String reason) {
        reasons.put(sessionId, reason);
    }

    /** Returns the registered reason and removes it, or null if none was set. */
    public String consumeReason(String sessionId) {
        return reasons.remove(sessionId);
    }
}
