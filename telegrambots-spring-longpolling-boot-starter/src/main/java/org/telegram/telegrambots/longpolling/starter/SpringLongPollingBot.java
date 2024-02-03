package org.telegram.telegrambots.longpolling.starter;

import org.telegram.telegrambots.longpolling.util.TelegramUpdateConsumer;

public interface SpringLongPollingBot {
    String getBotToken();
    TelegramUpdateConsumer getUpdatesConsumer();
}
