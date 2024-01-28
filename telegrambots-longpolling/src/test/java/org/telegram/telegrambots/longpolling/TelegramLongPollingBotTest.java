package org.telegram.telegrambots.longpolling;

import lombok.Setter;
import org.telegram.telegrambots.common.TelegramUrl;
import org.telegram.telegrambots.common.longpolling.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates;

@Setter
public class TelegramLongPollingBotTest implements TelegramLongPollingBot {
    private TelegramUrl baseUrl;

    @Override
    public void deleteWebhook() {

    }

    @Override
    public GetUpdates getUpdatesRequest(int currentOffset) {
        return GetUpdates.builder().offset(currentOffset + 1).limit(100).timeout(50).build();
    }

    @Override
    public String getBotToken() {
        return "TESTTOKEN";
    }


    @Override
    public TelegramUrl getBaseUrl() {
        return baseUrl;
    }

}
