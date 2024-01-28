package org.telegram.telegrambots.common.webhook;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

/**
 * @author Ruben Bermudez
 * @version 1.0
 */
public interface TelegramWebhookBot {
    WebhookTelegramUpdateConsumer getUpdatesConsumer();

    /**
     * Execute setWebhook method to set up the url of the webhook
     *
     * @throws TelegramApiRequestException In case of error executing the request
     *
     * @apiNote In case of error here, either fix the setwebhook request or unregister the bot manually to avoid future errors
     */
    void setWebhook() throws TelegramApiException;

    /**
     * Execute deleteWebhook method delete the exiting webhook for the bot
     *
     * @throws TelegramApiRequestException In case of error executing the request
     *
     * @apiNote In case of error here, either fix the deleteWebhook request or unregister the bot manually to avoid future errors
     */
    void deleteWebhook() throws TelegramApiException;

    /**
     * Gets in the url for the webhook
     * @return path in the url
     */
    String getBotPath();
}
