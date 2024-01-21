package org.telegram.telegrambots.common.longpolling;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * @author Ruben Bermudez
 * @version 1.0
 */
public interface TelegramBotsLongPolling extends AutoCloseable {
    /**
     * Use this method to register a new bot in the long polling app.
     * Upon registration, the bot will be started.
     * @param telegramLongPollingBot New Bot to register
     *
     * @throws TelegramApiException if any issue registering the bot or the bot is already registered
     */
    void registerBot(TelegramLongPollingBot telegramLongPollingBot) throws TelegramApiException;

    /**
     * Use this method to unregister a bot in the long polling app.
     * The bot will be stopped before it get unregistered
     * @param telegramLongPollingBot Bot to unregister
     *
     * @throws TelegramApiException if any issue unregistering the bot or the bot is not registerd
     */
    void unregisterBot(TelegramLongPollingBot telegramLongPollingBot) throws TelegramApiException;

    /**
     * Checks if all bot sessions are running
     * @return True if they are running, False otherwise
     */
    boolean isRunning();

    /**
     * Starts the long polling app
     *
     * @throws TelegramApiException if any issue starting the process (it can be an issue starting one of the bots registered)
     */
    void start() throws TelegramApiException;

    /**
     * Stop the long polling app
     *
     * @throws TelegramApiException if any issue stopping the process (it can be an issue stopping one of the bots registered)
     */
    void stop() throws TelegramApiException;
}
