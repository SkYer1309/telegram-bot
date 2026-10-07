package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        int lastUpdateId = 0;

        for (Update update : updates) {
            lastUpdateId = update.updateId();

            // Проверяем, есть ли сообщение
            if (update.message() != null) {
                String text = update.message().text();
                Long chatId = update.message().chat().id();

                // Проверяем команду /start
                if (text != null && text.equals("/start")) {
                    String welcomeMessage = "Привет! \n\n" +
                            "Я бот-напоминалка. Отправь мне задачу в формате:\n" +
                            "ДД.ММ.ГГГГ ЧЧ:ММ Текст задачи\n\n" +
                            "Пример:\n" +
                            "01.01.2026 20:00 Сделать домашнюю работу";

                    SendMessage sendMessage = new SendMessage(chatId, welcomeMessage);
                    telegramBot.execute(sendMessage); // ← заменил bot на telegramBot

                    System.out.println("Отправлено приветственное сообщение пользователю: " + chatId);
                }
            }
        }

        return lastUpdateId;
    }

}
