package org.telegram.telegrambots.longpolling.util;

import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class TelegramUpdateConsumer {
    Executor updatesProcessorExecutor = Executors.newSingleThreadExecutor();

    public void consume(List<Update> updates) {
        updates.forEach(update -> updatesProcessorExecutor.execute(() -> consume(update)));
    }

    public abstract void consume(Update update);
}
