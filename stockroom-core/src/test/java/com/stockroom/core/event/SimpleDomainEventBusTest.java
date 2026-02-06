package com.stockroom.core.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class SimpleDomainEventBusTest {

    private final SimpleDomainEventBus bus = new SimpleDomainEventBus();

    @Test
    void deliversEventsToSubscribersOfTheirType() {
        List<StockChangedEvent> received = new ArrayList<>();
        bus.subscribe(StockChangedEvent.class, received::add);

        StockChangedEvent event = new StockChangedEvent(1L, "SKU-1", "RECEIPT", null);
        bus.publish(event);
        bus.publish(new CostRevaluedEvent(1L, null, null));

        assertThat(received).containsExactly(event);
    }

    @Test
    void deliversToSupertypeSubscribers() {
        List<Object> received = new ArrayList<>();
        bus.subscribe(Object.class, received::add);

        bus.publish("hello");

        assertThat(received).containsExactly("hello");
    }

    @Test
    void cancelledSubscriptionsStopReceiving() {
        List<Object> received = new ArrayList<>();
        DomainEventBus.Subscription subscription = bus.subscribe(Object.class, received::add);

        subscription.cancel();
        bus.publish("ignored");

        assertThat(received).isEmpty();
        assertThat(bus.subscriberCount()).isZero();
    }

    @Test
    void handlersRunInSubscriptionOrder() {
        List<String> calls = new ArrayList<>();
        bus.subscribe(String.class, s -> calls.add("first"));
        bus.subscribe(String.class, s -> calls.add("second"));

        bus.publish("go");

        assertThat(calls).containsExactly("first", "second");
    }

    @Test
    void rejectsNullEvents() {
        assertThatThrownBy(() -> bus.publish(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
