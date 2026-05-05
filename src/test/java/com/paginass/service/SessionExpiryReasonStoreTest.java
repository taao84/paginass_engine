package com.paginass.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionExpiryReasonStoreTest {

    private final SessionExpiryReasonStore store = new SessionExpiryReasonStore();

    @Test
    void consumeReason_returnsRegisteredReasonAndClearsIt() {
        store.register("session-123", "account_disabled");

        assertThat(store.consumeReason("session-123")).isEqualTo("account_disabled");
        assertThat(store.consumeReason("session-123")).isNull();
    }

    @Test
    void consumeReason_returnsNullWhenNothingRegistered() {
        assertThat(store.consumeReason("unknown-session")).isNull();
    }

    @Test
    void register_overwritesExistingReason() {
        store.register("session-abc", "concurrent_login");
        store.register("session-abc", "account_disabled");

        assertThat(store.consumeReason("session-abc")).isEqualTo("account_disabled");
    }

    @Test
    void independentSessions_doNotInterfere() {
        store.register("session-1", "concurrent_login");
        store.register("session-2", "account_disabled");

        assertThat(store.consumeReason("session-1")).isEqualTo("concurrent_login");
        assertThat(store.consumeReason("session-2")).isEqualTo("account_disabled");
    }
}
