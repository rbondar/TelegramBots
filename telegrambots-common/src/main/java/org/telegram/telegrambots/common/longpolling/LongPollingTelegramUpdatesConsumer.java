package org.telegram.telegrambots.common.longpolling;

import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

public interface LongPollingTelegramUpdatesConsumer {
    void consume(Update update);

    default void consume(List<Update> updates) {
        updates.forEach(this::consume);
    }
}
