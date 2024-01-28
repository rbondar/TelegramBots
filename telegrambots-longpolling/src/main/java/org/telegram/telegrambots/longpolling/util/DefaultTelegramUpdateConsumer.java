package org.telegram.telegrambots.longpolling.util;

import org.telegram.telegrambots.common.longpolling.LongPollingTelegramUpdatesConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class DefaultTelegramUpdateConsumer implements LongPollingTelegramUpdatesConsumer {
    Executor updatesProcessorExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void consume(List<Update> updates) {
        updates.forEach(update -> updatesProcessorExecutor.execute(() -> consume(update)));
    }
}
