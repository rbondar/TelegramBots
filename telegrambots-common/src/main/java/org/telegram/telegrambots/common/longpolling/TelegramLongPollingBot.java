package org.telegram.telegrambots.common.longpolling;

import org.telegram.telegrambots.meta.ApiConstants;
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.net.Proxy;
import java.util.List;

/**
 * @author Ruben Bermudez
 * @version 1.0
 */
public interface TelegramLongPollingBot {
    /**
     * This method is called when receiving multiple updates
     * @param updates Updates received
     *
     * @apiNote By default, updates will be handled over to {{@link #onUpdateReceived(Update)}} in order
     */
    default void onUpdatesReceived(List<Update> updates) {
        updates.forEach(this::onUpdateReceived);
    }

    /**
     * This method is called when receiving updates
     * @param update Update received
     */
    void onUpdateReceived(Update update);

    /**
     * Execute deleteWebhook method delete the exiting webhook for the bot so long polling can runs
     *
     * @throws TelegramApiRequestException In case of error executing the request
     *
     * @apiNote In case of error here, either fix the deleteWebhook request or unregister the bot manually to avoid future errors
     */
    void deleteWebhook() throws TelegramApiException;

    /**
     * Return teh instance of GetUpdates to be used when calling the server
     * @return GetUpdates request
     */
    GetUpdates getUpdatesRequest();

    /**
     * Bot token to be used when calling Telegram API
     * @return Bot token
     */
    String getBotToken();

    /**
     * Returns the proxy to use in the connections
     * @return Proxy to be used
     */
    default Proxy getProxy() {
        return null;
    };

    /**
     * Returns user for proxy authentication.
     * @return User for proxy authentication
     */
    default String getProxyUser() {
        return null;
    }

    /**
     * Returns password for proxy authentication.
     * @return Password for proxy authentication
     */
    default String getProxyPassword() {
        return null;
    }

    default String getBaseUrl() {
        return ApiConstants.BASE_URL;
    }
}
