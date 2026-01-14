package com.stockroom.core.event;

import java.util.function.Consumer;

/**
 * Synchronous in-process event bus. Handlers run on the publishing thread, inside the
 * publisher's transaction, in subscription order.
 */
public interface DomainEventBus {

    void publish(Object event);

    <T> Subscription subscribe(Class<T> type, Consumer<? super T> handler);

    interface Subscription {
        void cancel();
    }
}
