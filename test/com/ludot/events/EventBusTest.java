package com.ludot.events;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
@DisplayName("EventBus (Observer)")
class EventBusTest {
    @Test @DisplayName("subscribe adds listener") void subscribeAddsListener() {}
    @Test @DisplayName("publish calls onEvent on all subscribers") void publishCallsAll() {}
    @Test @DisplayName("multiple listeners each receive the event") void multipleListeners() {}
}