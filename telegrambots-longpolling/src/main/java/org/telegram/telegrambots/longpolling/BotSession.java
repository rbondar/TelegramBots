package org.telegram.telegrambots.longpolling;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Authenticator;
import okhttp3.ConnectionPool;
import okhttp3.Credentials;
import okhttp3.Dispatcher;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Route;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.telegram.telegrambots.common.longpolling.TelegramLongPollingBot;
import org.telegram.telegrambots.longpolling.exceptions.TelegramApiErrorResponseException;
import org.telegram.telegrambots.longpolling.util.ExponentialBackOff;
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.BackOff;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Optional.ofNullable;

@Data
@Slf4j
public class BotSession implements AutoCloseable {
    private final BackOff backOff = new ExponentialBackOff();

    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicInteger lastReceivedUpdate = new AtomicInteger(0);

    private final TelegramLongPollingBot telegramLongPollingBot;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final ScheduledExecutorService executor;

    private volatile ScheduledFuture<?> runningPolling = null;

    public BotSession(TelegramLongPollingBot telegramLongPollingBot,
                      ObjectMapper objectMapper,
                      OkHttpClient httpClient,
                      ScheduledExecutorService executor) {
        this.telegramLongPollingBot = telegramLongPollingBot;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.executor = executor == null ? Executors.newSingleThreadScheduledExecutor() : executor;
        okHttpClient = httpClient == null ? createHttpClient(telegramLongPollingBot) : httpClient;
    }

    public void start() throws TelegramApiException {
        if (runningPolling == null) {
            runningPolling = createPollerTask();
        }
    }

    public void stop() throws TelegramApiException {
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
                if (!updates.isEmpty()) {
                    updates.removeIf(x -> x.getUpdateId() < lastReceivedUpdate.get());
                    lastReceivedUpdate.set(updates.parallelStream()
                            .mapToInt(Update::getUpdateId)
                            .max()
                            .orElse(0));
                    telegramLongPollingBot.onUpdatesReceived(updates);
                }
            } catch (TelegramApiRequestException | IOException e) {
                log.error(e.getLocalizedMessage(), e);
            } catch (TelegramApiErrorResponseException e) {
                long backOffMillis = backOff.nextBackOffMillis();
                log.error("Error received from Telegram GetUpdates Request, retrying in {} millis...", backOffMillis, e);
                try {
                    Thread.sleep(backOffMillis);
                } catch (InterruptedException ex) {
                    // Ignore this
                    log.warn("GetUpdates got interrupted while sleeping in backoff mode.", ex);
                }
            }
        }, 1, 1, TimeUnit.MILLISECONDS);
    }

    private List<Update> getUpdatesFromTelegram() throws TelegramApiRequestException, IOException, TelegramApiErrorResponseException {
        Request request = new Request.Builder()
                .url(
                        new HttpUrl
                                .Builder()
                                .host(telegramLongPollingBot.getBaseUrl() + telegramLongPollingBot.getBotToken())
                                .addPathSegment(GetUpdates.PATH)
                                .build()
                )
                .header("charset", StandardCharsets.UTF_8.name())
                .post(RequestBody.create(objectMapper.writeValueAsString(telegramLongPollingBot.getUpdatesRequest()), MediaType.parse("application/json")))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                try (ResponseBody body = response.body()) {
                    if (body != null) {
                        List<Update> updates = telegramLongPollingBot.getUpdatesRequest().deserializeResponse(body.string());
                        // Reset backup with every successful request
                        backOff.reset();
                        return updates;
                    }
                }
            } else {
                throw new TelegramApiErrorResponseException(response.code(), response.message());
            }
        }

        return Collections.emptyList();
    }

    @NonNull
    private static OkHttpClient createHttpClient(TelegramLongPollingBot telegramLongPollingBot) {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(100); // Max requests
        dispatcher.setMaxRequestsPerHost(100); // Max per host

        final OkHttpClient okHttpClient;
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient()
                .newBuilder()
                .dispatcher(dispatcher)
                .connectionPool(new ConnectionPool(
                        100, // Max conn #
                        75, // Keepaliave
                        TimeUnit.SECONDS
                ))
                .readTimeout(100, TimeUnit.SECONDS) // Time to read from server
                .writeTimeout(70, TimeUnit.SECONDS) // Time to write to server
                .connectTimeout(75, TimeUnit.SECONDS); // Max Connect timeout
        // .callTimeout(200, TimeUnit.SECONDS) // Overall timeout, needed?;

        ofNullable(telegramLongPollingBot.getProxy()).ifPresent(proxy -> { // Proxy
            okHttpClientBuilder.proxy(proxy);
            if (StringUtils.isNotBlank(telegramLongPollingBot.getProxyUser()) && StringUtils.isNotBlank(telegramLongPollingBot.getProxyPassword())) {
                okHttpClientBuilder.proxyAuthenticator(getProxyAuthenticator(telegramLongPollingBot.getProxyUser(), telegramLongPollingBot.getProxyPassword()));
            }
        });

        return okHttpClientBuilder.build();
    }

    @NonNull
    private static Authenticator getProxyAuthenticator(String user, String password) {
        return new Authenticator() {
            @Override
            public @NonNull Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {
                String credential = Credentials.basic(user, password);
                return response
                        .request()
                        .newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build();
            }
        };
    }
}
