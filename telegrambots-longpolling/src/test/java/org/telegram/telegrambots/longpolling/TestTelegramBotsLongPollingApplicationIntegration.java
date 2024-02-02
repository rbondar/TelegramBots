package org.telegram.telegrambots.longpolling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.longpolling.util.DefaultGetUpdatesGenerator;
import org.telegram.telegrambots.longpolling.util.TelegramUpdateConsumer;
import org.telegram.telegrambots.meta.TelegramUrl;
import org.telegram.telegrambots.meta.api.objects.ApiResponse;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

public class TestTelegramBotsLongPollingApplicationIntegration {
    private MockWebServer webServer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private TelegramUrl telegramUrl;
    private TelegramBotsLongPollingApplication application;

    @BeforeEach
    public void setUp() {
        webServer = new MockWebServer();
        HttpUrl mockUrl = webServer.url("");
        telegramUrl = TelegramUrl.builder().schema(mockUrl.scheme()).host(mockUrl.host()).port(mockUrl.port()).build();
        application = new TelegramBotsLongPollingApplication();
    }

    @AfterEach
    public void tearDown() throws Exception {
        application.close();
    }

    @Test
    public void test1() {
        try {
            List<Update> updateReceived = new ArrayList<>();

            Dispatcher dispatcher = new Dispatcher() {
                @NonNull
                @Override
                public MockResponse dispatch(@NonNull RecordedRequest request) throws InterruptedException {
                    try {
                        switch (request.getPath()) {
                            case "/botTOKEN/deleteWebhook":
                                return mockResponse(ApiResponse.<Boolean>builder().ok(true).result(true).build());
                            case "/botTOKEN/getupdates":
                                if (request.getSequenceNumber() == 1) {
                                    return mockResponse(getFakeUpdates1());
                                } else {
                                    return mockResponse(getFakeUpdates2());
                                }
                        }
                    } catch (Exception e) {
                        return new MockResponse().setResponseCode(404);
                    }
                    return new MockResponse().setResponseCode(404);
                }
            };
            webServer.setDispatcher(dispatcher);

            application.registerBot("TOKEN",
                    () -> telegramUrl,
                    new DefaultGetUpdatesGenerator(),
                    new TelegramUpdateConsumer() {
                        public void consume(Update update) {
                            updateReceived.add(update);
                        }
                    });

            await().atMost(5, TimeUnit.SECONDS).until(() -> updateReceived.size() == 4);
        } catch (Exception e) {
            fail(e);
        }
    }

    private MockResponse mockResponse(Object responseObject) throws JsonProcessingException {
        return new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .setBody(objectMapper.writeValueAsString(responseObject));
    }

    private ApiResponse<List<Update>> getFakeUpdates1() {
        Update update1 = new Update();
        update1.setUpdateId(1);
        Update update2 = new Update();
        update2.setUpdateId(2);
        return ApiResponse.<List<Update>>builder().ok(true).result(List.of(update1, update2)).build();
    }

    private ApiResponse<List<Update>> getFakeUpdates2() {
        Update update1 = new Update();
        update1.setUpdateId(3);
        Update update2 = new Update();
        update2.setUpdateId(4);
        return ApiResponse.<List<Update>>builder().ok(true).result(List.of(update1, update2)).build();
    }
}
