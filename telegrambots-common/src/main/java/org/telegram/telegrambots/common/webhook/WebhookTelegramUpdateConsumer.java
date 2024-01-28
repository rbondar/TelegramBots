package org.telegram.telegrambots.common.webhook;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

public abstract class WebhookTelegramUpdateConsumer {
    public abstract BotApiMethod<?> consume(Update update);
}
