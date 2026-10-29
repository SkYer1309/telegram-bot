package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.entity.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements com.pengrad.telegrambot.UpdatesListener {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    // Паттерн для распознавания формата: ДД.ММ.ГГГГ ЧЧ:ММ Текст задачи
    private static final Pattern TASK_PATTERN = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4}\\s\\d{2}:\\d{2})(\\s+)(.+)");

    // Формат даты для парсинга
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Autowired
    private TelegramBot telegramBot;

    @Autowired
    private NotificationTaskRepository notificationTaskRepository;

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

                // Обработка команды /start
                if (text != null && text.equals("/start")) {
                    sendWelcomeMessage(chatId);
                }
                // Обработка сообщений с задачами
                else if (text != null) {
                    processTaskMessage(text, chatId);
                }
            }
        }

        return lastUpdateId;
    }

    /**
     * Отправляет приветственное сообщение пользователю
     */
    private void sendWelcomeMessage(Long chatId) {
        String welcomeMessage = "Привет! 👋\n\n" +
                "Я бот-напоминалка. Отправь мне задачу в формате:\n" +
                "<code>ДД.ММ.ГГГГ ЧЧ:ММ Текст задачи</code>\n\n" +
                "Пример:\n" +
                "<code>01.01.2026 20:00 Сделать домашнюю работу</code>\n\n" +
                "Я напомню тебе о задаче в указанное время! ⏰";

        SendMessage sendMessage = new SendMessage(chatId, welcomeMessage);
        telegramBot.execute(sendMessage);

        log.info("Отправлено приветственное сообщение пользователю: {}", chatId);
    }

    /**
     * Обрабатывает сообщение с задачей: парсит, валидирует и сохраняет в БД
     */
    private void processTaskMessage(String text, Long chatId) {
        Matcher matcher = TASK_PATTERN.matcher(text);

        if (!matcher.matches()) {
            // Если формат неверный — сообщаем пользователю
            String errorMessage = "❌ Неверный формат сообщения!\n\n" +
                    "Используйте формат:\n" +
                    "<code>ДД.ММ.ГГГГ ЧЧ:ММ Текст задачи</code>\n\n" +
                    "Пример:\n" +
                    "<code>01.01.2026 20:00 Сделать домашнюю работу</code>";

            SendMessage sendMessage = new SendMessage(chatId, errorMessage);
            telegramBot.execute(sendMessage);

            log.warn("Неверный формат сообщения от пользователя {}: {}", chatId, text);
            return;
        }

        // Извлекаем дату/время и текст задачи из совпадений
        String dateTimeStr = matcher.group(1);  // "01.01.2026 20:00"
        String taskText = matcher.group(3);     // "Сделать домашнюю работу"

        // Парсим строку в объект LocalDateTime
        LocalDateTime notificationTime;
        try {
            notificationTime = LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            String errorMessage = "❌ Ошибка при парсинге даты!\n\nУбедитесь, что дата и время корректны.";
            telegramBot.execute(new SendMessage(chatId, errorMessage));
            log.error("Ошибка парсинга даты: {}", dateTimeStr, e);
            return;
        }

        // Проверяем, что дата не в прошлом
        if (notificationTime.isBefore(LocalDateTime.now())) {
            String errorMessage = "⚠️ Указанное время уже прошло!\n\nПожалуйста, укажите время в будущем.";
            telegramBot.execute(new SendMessage(chatId, errorMessage));
            log.warn("Попытка создать задачу в прошлом: {} от пользователя {}", dateTimeStr, chatId);
            return;
        }

        // Создаём сущность и сохраняем в БД
        NotificationTask task = new NotificationTask();
        task.setChatId(chatId);
        task.setMessageText(taskText);
        task.setNotificationTime(notificationTime);
        task.setIsSent(false);

        NotificationTask savedTask = notificationTaskRepository.save(task);

        log.info("Задача сохранена в БД: id={}, chatId={}, time={}, text={}",
                savedTask.getId(), chatId, notificationTime, taskText);

        // Отправляем подтверждение пользователю
        String confirmationMessage = String.format(
                "✅ Задача сохранена!\n\n" +
                        "📝 Текст: <b>%s</b>\n" +
                        "⏰ Напомню: <b>%s</b>",
                taskText,
                notificationTime.format(DATE_TIME_FORMATTER)
        );

        SendMessage sendMessage = new SendMessage(chatId, confirmationMessage);
        telegramBot.execute(sendMessage);
    }
}