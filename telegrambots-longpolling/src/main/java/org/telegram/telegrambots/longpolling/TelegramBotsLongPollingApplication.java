package org.telegram.telegrambots.longpolling;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.telegram.telegrambots.common.TelegramUrl;
import org.telegram.telegrambots.common.longpolling.LongPollingTelegramUpdatesConsumer;
import org.telegram.telegrambots.common.longpolling.TelegramBotsLongPolling;
import org.telegram.telegrambots.longpolling.util.DefaultGetUpdatesGenerator;
import org.telegram.telegrambots.longpolling.util.DefaultTelegramUpdateConsumer;
import org.telegram.telegrambots.longpolling.util.TelegramOkHttpClientFactory;
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class TelegramBotsLongPollingApplication implements TelegramBotsLongPolling {
    private final Supplier<ObjectMapper> objectMapperSupplier;
    private final Supplier<OkHttpClient> okHttpClientCreator;
    private final Supplier<ScheduledExecutorService> executorSupplier;

    private final ConcurrentHashMap<String, BotSession> botSessions = new ConcurrentHashMap<>();
    public TelegramBotsLongPollingApplication() {
        this(ObjectMapper::new);
    }

    public TelegramBotsLongPollingApplication(Supplier<ObjectMapper> objectMapperSupplier) {
        this(objectMapperSupplier, new TelegramOkHttpClientFactory.DefaultOkHttpClientCreator());
    }

    public TelegramBotsLongPollingApplication(Supplier<ObjectMapper> objectMapperSupplier, Supplier<OkHttpClient> okHttpClientCreator) {
        this(objectMapperSupplier, okHttpClientCreator, Executors::newSingleThreadScheduledExecutor);
    }

    public TelegramBotsLongPollingApplication(Supplier<ObjectMapper> objectMapperSupplier,
                                              Supplier<OkHttpClient> okHttpClientCreator,
                                              Supplier<ScheduledExecutorService> executorSupplier) {
        this.objectMapperSupplier = objectMapperSupplier;
        this.okHttpClientCreator = okHttpClientCreator;
        this.executorSupplier = executorSupplier;
    }

    public void registerBot(String botToken, DefaultTelegramUpdateConsumer defaultUpdatesConsumer) throws TelegramApiException {
        registerBot(botToken, () -> TelegramUrl.DEFAULT_URL, new DefaultGetUpdatesGenerator(), defaultUpdatesConsumer);
    }

    @Override
    public void registerBot(String botToken,
                            Supplier<TelegramUrl> telegramUrlSupplier,
                            Function<Integer, GetUpdates> getUpdatesGenerator,
                            LongPollingTelegramUpdatesConsumer updatesConsumer) throws TelegramApiException {
        if (botSessions.containsKey(botToken)) {
            throw new TelegramApiException("Bot is already registered");
        } else {
            BotSession botSession = new BotSession(
                    objectMapperSupplier.get(),
                    okHttpClientCreator.get(),
                    executorSupplier.get(),
                    botToken,
                    telegramUrlSupplier,
                    getUpdatesGenerator,
                    updatesConsumer);
            botSessions.put(botToken, botSession);
            botSession.start();
        }
    }

    @Override
    public void unregisterBot(String botToken) throws TelegramApiException {
        if (botSessions.containsKey(botToken)) {
            BotSession botSession = botSessions.remove(botToken);
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
        for (BotSession botSession : botSessions.values()) {
            if (botSession != null) {
                botSession.close();
            }
        }
    }
}
