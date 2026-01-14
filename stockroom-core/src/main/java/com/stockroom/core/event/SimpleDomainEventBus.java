package com.stockroom.core.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimpleDomainEventBus implements DomainEventBus {

    private static final Logger log = LoggerFactory.getLogger(SimpleDomainEventBus.class);

    private final List<Registration<?>> registrations = new CopyOnWriteArrayList<>();

    @Override
    public void publish(Object event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        for (Registration<?> registration : registrations) {
            registration.deliver(event);
        }
        log.debug("Published {}", event.getClass().getSimpleName());
    }

    @Override
    public <T> Subscription subscribe(Class<T> type, Consumer<? super T> handler) {
        Registration<T> registration = new Registration<>(type, handler);
        registrations.add(registration);
        return () -> registrations.remove(registration);
    }

    int subscriberCount() {
        return registrations.size();
    }

    private record Registration<T>(Class<T> type, Consumer<? super T> handler) {

        void deliver(Object event) {
            if (type.isInstance(event)) {
                handler.accept(type.cast(event));
            }
        }
    }
}
