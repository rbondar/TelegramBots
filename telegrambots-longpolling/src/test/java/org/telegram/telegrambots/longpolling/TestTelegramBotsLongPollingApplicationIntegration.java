package org.telegram.telegrambots.longpolling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.common.TelegramUrl;
import org.telegram.telegrambots.common.longpolling.LongPollingTelegramUpdatesConsumer;
import org.telegram.telegrambots.meta.api.objects.ApiResponse;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestTelegramBotsLongPollingApplicationIntegration {
    private MockWebServer webServer;
    private ObjectMapper objectMapper = new ObjectMapper();

    private TelegramLongPollingBotTest telegramLongPollingBotTest;

    private TelegramBotsLongPollingApplication application;

    @BeforeEach
    public void setUp() {
        telegramLongPollingBotTest = new TelegramLongPollingBotTest();
        webServer = new MockWebServer();
        HttpUrl mockUrl = webServer.url("");
        telegramLongPollingBotTest.setBaseUrl(TelegramUrl.builder().schema(mockUrl.scheme()).host(mockUrl.host()).port(mockUrl.port()).build());
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

            webServer.enqueue(mockResponse(getFakeUpdates1()));
            webServer.enqueue(mockResponse(getFakeUpdates2()));

            application.registerBot(telegramLongPollingBotTest, new LongPollingTelegramUpdatesConsumer() {
                @Override
                public void consume(Update update) {
                    updateReceived.add(update);
                }
            });

            await().atMost(5, TimeUnit.SECONDS).until(() -> updateReceived.size() == 4);

            assertEquals(4, updateReceived.size());
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
