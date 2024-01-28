package org.telegram.telegrambots.common.longpolling;

import org.telegram.telegrambots.common.TelegramUrl;
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

/**
 * @author Ruben Bermudez
 * @version 1.0
 */
public interface TelegramLongPollingBot {
    /**
     * Execute deleteWebhook method delete the exiting webhook for the bot so long polling can runs
     *
     * @throws TelegramApiRequestException In case of error executing the request
     *
     * @apiNote In case of error here, either fix the deleteWebhook request or unregister the bot manually to avoid future errors
     */
    void deleteWebhook() throws TelegramApiException;

    /**
     * Return the instance of GetUpdates to be used when calling the server
     * @return GetUpdates request
     */
    GetUpdates getUpdatesRequest(int currentOffset);

    /**
     * Bot token to be used when calling Telegram API
     * @return Bot token
     */
    String getBotToken();

    /**
     * Base URL to use in getUpdates request
     * @return The base URL to be used
     */
    default TelegramUrl getBaseUrl() {
        return TelegramUrl.DEFAULT_URL;
    }
}
