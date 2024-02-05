package org.telegram.telegrambots.session;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

@SuppressWarnings({"WeakerAccess", "OptionalUsedAsFieldOrParameterType", "unused"})
public abstract class TelegramLongPollingSessionBot implements LongPollingSingleThreadUpdateConsumer {
    DefaultSessionManager sessionManager;

    ChatIdConverter chatIdConverter;

    public TelegramLongPollingSessionBot(){
        this(new DefaultChatIdConverter());
    }

    public TelegramLongPollingSessionBot(ChatIdConverter chatIdConverter){
        this.setSessionManager(new DefaultSessionManager());
        this.setChatIdConverter(chatIdConverter);
        AbstractSessionDAO sessionDAO = (AbstractSessionDAO) sessionManager.getSessionDAO();
        sessionDAO.setSessionIdGenerator(chatIdConverter);
    }


    public void setSessionManager(DefaultSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void setChatIdConverter(ChatIdConverter chatIdConverter) {
        this.chatIdConverter = chatIdConverter;
    }

    @Override
    public void consume(Update update) {
        Optional<Session> chatSession;
        Message message = null;
        Long chatId = null;
        if (update.hasMessage()) {
            chatIdConverter.setSessionId(update.getMessage().getChatId());
            chatSession = this.getSession(update.getMessage());
            onUpdateReceived(update, chatSession);
        } if (update.hasCallbackQuery() && update.getCallbackQuery().getMessage() != null) {
            chatIdConverter.setSessionId(update.getCallbackQuery().getMessage().getChatId());

        } else {
            chatSession = Optional.empty();
            onUpdateReceived(update, chatSession);
        }
    }

    public Optional<Session> getSession(Message message){
        try {
            return Optional.of(sessionManager.getSession(chatIdConverter));
        } catch (UnknownSessionException e) {
            SessionContext botSession = new DefaultChatSessionContext(message.getChatId(), message.getFrom().getUserName());
            return Optional.of(sessionManager.start(botSession));
        }
    }

    public abstract void onUpdateReceived(Update update, Optional<Session> botSession);
}
