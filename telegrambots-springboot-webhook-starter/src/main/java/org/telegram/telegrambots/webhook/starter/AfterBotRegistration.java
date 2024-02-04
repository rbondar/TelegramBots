package org.telegram.telegrambots.webhook.starter;

import org.telegram.telegrambots.webhook.TelegramBotsWebhookApplication;
import org.telegram.telegrambots.webhook.TelegramWebhookBot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicated that the Method of a Class extending {@link TelegramWebhookBot} will be called after the bot was registered
 * <br><br>
 * <p>The bot session passed is the ones returned by {@link TelegramBotsWebhookApplication#registerBot(TelegramWebhookBot)}</p
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AfterBotRegistration {}
