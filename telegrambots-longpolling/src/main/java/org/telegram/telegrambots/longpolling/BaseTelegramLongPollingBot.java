package org.telegram.telegrambots.longpolling;

import org.telegram.telegrambots.common.longpolling.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class BaseTelegramLongPollingBot implements TelegramLongPollingBot {
    Executor updatesProcessorExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        updates.forEach(update -> updatesProcessorExecutor.execute(() -> onUpdateReceived(update)));
    }
}
