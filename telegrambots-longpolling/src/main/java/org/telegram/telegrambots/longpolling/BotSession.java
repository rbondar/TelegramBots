package org.telegram.telegrambots.longpolling;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.telegram.telegrambots.common.longpolling.LongPollingTelegramUpdatesConsumer;
import org.telegram.telegrambots.common.longpolling.TelegramLongPollingBot;
import org.telegram.telegrambots.longpolling.exceptions.TelegramApiErrorResponseException;
import org.telegram.telegrambots.longpolling.util.ExponentialBackOff;
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.BackOff;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@Slf4j
public class BotSession implements AutoCloseable {
    private final BackOff backOff = new ExponentialBackOff();
    private final LongPollingTelegramUpdatesConsumer updatesConsumer;

    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicInteger lastReceivedUpdate = new AtomicInteger(0);

    private final TelegramLongPollingBot telegramLongPollingBot;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final ScheduledExecutorService executor;

    private volatile ScheduledFuture<?> runningPolling = null;

    public BotSession(TelegramLongPollingBot telegramLongPollingBot,
                      LongPollingTelegramUpdatesConsumer updatesConsumer,
                      ObjectMapper objectMapper,
                      OkHttpClient okHttpClient,
                      ScheduledExecutorService executor) {
        this.telegramLongPollingBot = telegramLongPollingBot;
        this.objectMapper = objectMapper;
        this.executor = executor;
        this.okHttpClient = okHttpClient;
        this.updatesConsumer = updatesConsumer;
    }

    public void start() throws TelegramApiException {
        if (runningPolling == null) {
            runningPolling = createPollerTask();
        }
    }

    public void stop() {
        if (runningPolling != null) {
            runningPolling.cancel(false);
            runningPolling = null;
        }
    }

    public boolean isRunning() {
        return runningPolling != null && !runningPolling.isCancelled();
    }

    @Override
    public void close() throws TelegramApiException {
        stop();
    }

    @NonNull
    private ScheduledFuture<?> createPollerTask() throws TelegramApiException {
        telegramLongPollingBot.deleteWebhook();
        return executor.scheduleAtFixedRate(() -> {
            try {
                List<Update> updates = getUpdatesFromTelegram();
                // Reset backup with every successful request
                backOff.reset();
                // Handle updates
                if (!updates.isEmpty()) {
                    updates.removeIf(x -> x.getUpdateId() < lastReceivedUpdate.get());
                    lastReceivedUpdate.set(updates.parallelStream()
                            .mapToInt(Update::getUpdateId)
                            .max()
                            .orElse(0));
                    updatesConsumer.consume(updates);
                }
            } catch (TelegramApiErrorResponseException e) {
                long backOffMillis = backOff.nextBackOffMillis();
                log.error("Error received from Telegram GetUpdates Request, retrying in {} millis...", backOffMillis, e);
                try {
                    Thread.sleep(backOffMillis);
                } catch (InterruptedException ex) {
                    // Ignore this
                    log.warn("GetUpdates got interrupted while sleeping in backoff mode.", ex);
                }
            } catch (TelegramApiException  e) {
                log.error(e.getLocalizedMessage(), e);
            }
        }, 1, 1, TimeUnit.MICROSECONDS);
    }

    private List<Update> getUpdatesFromTelegram() throws TelegramApiRequestException, TelegramApiErrorResponseException {
        try {
            Request request = new Request.Builder()
                    .url(
                            new HttpUrl
                                    .Builder()
                                    .scheme(telegramLongPollingBot.getBaseUrl().getSchema())
                                    .host(telegramLongPollingBot.getBaseUrl().getHost())
                                    .port(telegramLongPollingBot.getBaseUrl().getPort())
                                    .addPathSegment("bot" + telegramLongPollingBot.getBotToken())
                                    .addPathSegment(GetUpdates.PATH)
                                    .build()
                    )
                    .header("charset", StandardCharsets.UTF_8.name())
                    .post(RequestBody.create(objectMapper.writeValueAsString(telegramLongPollingBot.getUpdatesRequest(lastReceivedUpdate.get())), MediaType.parse("application/json")))
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    try (ResponseBody body = response.body()) {
                        if (body != null) {
                            List<Update> updates = telegramLongPollingBot.getUpdatesRequest(lastReceivedUpdate.get()).deserializeResponse(body.string());
                            // Reset backup with every successful request
                            backOff.reset();
                            return updates;
                        }
                    }
                } else {
                    throw new TelegramApiErrorResponseException(response.code(), response.message());
                }
            }
        } catch (Exception e) {
            throw new TelegramApiErrorResponseException(e);
        }

        return Collections.emptyList();
    }
}
