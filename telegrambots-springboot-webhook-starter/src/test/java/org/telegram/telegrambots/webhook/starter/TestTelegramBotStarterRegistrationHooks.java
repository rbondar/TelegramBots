package org.telegram.telegrambots.webhook.starter;

import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.webhook.TelegramBotsWebhookApplication;
import org.telegram.telegrambots.webhook.TelegramWebhookBot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class TestTelegramBotStarterRegistrationHooks {

    private static final TelegramBotsWebhookApplication mockApplication = mock(TelegramBotsWebhookApplication.class);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withAllowBeanDefinitionOverriding(true)
            .withConfiguration(AutoConfigurations.of(MockTelegramApplication.class,
                                                     TelegramBotStarterConfiguration.class));

    @Test
    void createOnlyWebhookBot() {
        this.contextRunner.withUserConfiguration(WebookBotConfig.class)
                .run((context) -> {
                    assertThat(context).hasSingleBean(AnnotatedWebhookBot.class);

                    TelegramBotsWebhookApplication telegramApplication = context.getBean(TelegramBotsWebhookApplication.class);
                    TelegramWebhookBot bot = context.getBean(TelegramWebhookBot.class);

                    verify(telegramApplication, times(1)).registerBot(eq(bot));
                    verifyNoMoreInteractions(telegramApplication);
                    assertInstanceOf(AnnotatedWebhookBot.class, bot);
                    assertTrue(((AnnotatedWebhookBot) bot).isHookCalled());
                });
    }


    @Configuration
    public static class MockTelegramApplication {
        @Bean
        public TelegramBotsWebhookApplication telegramBotsApplication() {
            return mockApplication;
        }
    }

    @Configuration
    public static class WebookBotConfig {
        @Bean
        public TelegramWebhookBot longPollingBot() {
            return new AnnotatedWebhookBot();
        }
    }

    @Getter
    public static class AnnotatedWebhookBot extends TelegramWebhookBot {
        private boolean hookCalled = false;

        public AnnotatedWebhookBot() {
            super("TEST", update -> null, () -> { }, () -> { });
        }

        @AfterBotRegistration
        public void afterBotHook() {
            hookCalled = true;
        }
    }
}
