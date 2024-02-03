package org.telegram.telegrambots.longpolling.starter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.TelegramUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class TestTelegramBotStarterConfiguration {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withAllowBeanDefinitionOverriding(true)
            .withConfiguration(AutoConfigurations.of(MockTelegramBotsLongPollingApplication.class,
                                                     TelegramBotStarterConfiguration.class));

    @Test
    void createMockTelegramBotsApiWithDefaultSettings() {
        this.contextRunner.run((context) -> {
            assertThat(context).hasSingleBean(TelegramBotsLongPollingApplication.class);
            assertThat(context).hasSingleBean(TelegramBotInitializer.class);
            assertThat(context).doesNotHaveBean(SpringLongPollingBot.class);
            verifyNoMoreInteractions(context.getBean(TelegramBotsLongPollingApplication.class));
        });
    }

    @Test
    void createOnlyLongPollingBot() {
        this.contextRunner.withUserConfiguration(LongPollingBotConfig.class)
                .run((context) -> {
                    assertThat(context).hasSingleBean(SpringLongPollingBot.class);

                    TelegramBotsLongPollingApplication telegramBotsApi = context.getBean(TelegramBotsLongPollingApplication.class);

                    verify(telegramBotsApi, times(1)).registerBot(anyString(), any(TelegramUpdateConsumer.class));
                    verifyNoMoreInteractions(telegramBotsApi);
                });
    }

    @Configuration
    static class MockTelegramBotsLongPollingApplication {

        @Bean
        public TelegramBotsLongPollingApplication telegramBotsApplication() {
            return mock(TelegramBotsLongPollingApplication.class);
        }
    }

    @Configuration
    static class LongPollingBotConfig {
        @Bean
        public SpringLongPollingBot longPollingBot() {
            SpringLongPollingBot springLongPollingBotMock = mock(SpringLongPollingBot.class);
            doReturn("").when(springLongPollingBotMock).getBotToken();
            doReturn(new TelegramUpdateConsumer() {
                @Override
                public void consume(Update update) {

                }
            }).when(springLongPollingBotMock).getUpdatesConsumer();
            return springLongPollingBotMock;
        }
    }
}
