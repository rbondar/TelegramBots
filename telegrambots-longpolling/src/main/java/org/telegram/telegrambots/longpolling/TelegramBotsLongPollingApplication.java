package org.telegram.telegrambots.longpolling;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.common.longpolling.TelegramBotsLongPolling;
import org.telegram.telegrambots.common.longpolling.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TelegramBotsLongPollingApplication implements TelegramBotsLongPolling {
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, BotSession> botSessions = new ConcurrentHashMap<>();

    public TelegramBotsLongPollingApplication() {
        this(new ObjectMapper());
    }

    public TelegramBotsLongPollingApplication(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void registerBot(TelegramLongPollingBot telegramLongPollingBot) throws TelegramApiException {
        if (botSessions.containsKey(telegramLongPollingBot.getBotToken())) {
            throw new TelegramApiException("Bot is already registered");
        } else {
            BotSession botSession = new BotSession(telegramLongPollingBot, objectMapper);
            botSessions.put(telegramLongPollingBot.getBotToken(), botSession);
            botSession.start();
        }
    }

    @Override
    public void unregisterBot(TelegramLongPollingBot telegramLongPollingBot) throws TelegramApiException {
        if (botSessions.containsKey(telegramLongPollingBot.getBotToken())) {
            BotSession botSession = botSessions.remove(telegramLongPollingBot.getBotToken());
            botSession.stop();
        } else {
            throw new TelegramApiException("Bot is not registered");
        }
    }

    @Override
    public boolean isRunning() {
        return botSessions.values().stream().allMatch(BotSession::isRunning);
    }

    @Override
    public void start() throws TelegramApiException {
        if (botSessions.values().stream().allMatch(BotSession::isRunning)) {
            throw new TelegramApiException("All bots already running");
        }
        for (BotSession botSession : botSessions.values()) {
            if (!botSession.isRunning()) {
                botSession.start();
            }
        }
    }

    @Override
    public void stop() throws TelegramApiException {
        if (botSessions.values().stream().noneMatch(BotSession::isRunning)) {
            throw new TelegramApiException("All bots already running");
        }
        for (BotSession botSession : botSessions.values()) {
            if (botSession.isRunning()) {
                botSession.stop();
            }
        }
    }

    @Override
    public void close() throws Exception {
        this.stop();
        for (BotSession botSession : botSessions.values()) {
            if (botSession != null) {
                botSession.close();
            }
        }
    }
}
