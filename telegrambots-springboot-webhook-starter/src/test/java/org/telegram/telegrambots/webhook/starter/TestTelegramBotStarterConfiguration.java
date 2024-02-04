package org.telegram.telegrambots.webhook.starter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.webhook.TelegramBotsWebhookApplication;
import org.telegram.telegrambots.webhook.TelegramWebhookBot;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class TestTelegramBotStarterConfiguration {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withAllowBeanDefinitionOverriding(true)
            .withConfiguration(AutoConfigurations.of(MockTelegramApplication.class,
                                                     TelegramBotStarterConfiguration.class));

    @Test
    void createMockTelegramApplicationWithDefaultSettings() {
        this.contextRunner.run((context) -> {
            assertThat(context).hasSingleBean(TelegramBotsWebhookApplication.class);
            assertThat(context).hasSingleBean(TelegramBotInitializer.class);
            assertThat(context).doesNotHaveBean(TelegramWebhookBot.class);
            verifyNoMoreInteractions(context.getBean(TelegramBotsWebhookApplication.class));
        });
    }

    @Test
    void createOnlyLongPollingBot() {
        this.contextRunner.withUserConfiguration(LongPollingBotConfig.class)
                .run((context) -> {
                    assertThat(context).hasSingleBean(TelegramWebhookBot.class);

                    TelegramBotsWebhookApplication telegramApplication = context.getBean(TelegramBotsWebhookApplication.class);

                    verify(telegramApplication, times(1)).registerBot(any(TelegramWebhookBot.class));
                    verifyNoMoreInteractions(telegramApplication);
                });
    }

    @Configuration
    static class MockTelegramApplication {
        @Bean
        public TelegramBotsWebhookApplication telegramBotsApplication() {
            return mock(TelegramBotsWebhookApplication.class);
        }
    }

    @Configuration
    static class LongPollingBotConfig {
        @Bean
        public TelegramWebhookBot webhookBot() {
            TelegramWebhookBot springLongPollingBotMock = mock(TelegramWebhookBot.class);
            doReturn("").when(springLongPollingBotMock).getBotPath();
            doReturn((Function<Update, BotApiMethod<?>>) update -> null).when(springLongPollingBotMock).getUpdateHandler();
            return springLongPollingBotMock;
        }
    }
}
