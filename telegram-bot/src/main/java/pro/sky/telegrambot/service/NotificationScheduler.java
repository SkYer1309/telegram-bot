package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.entity.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final TelegramBot telegramBot;
    private final NotificationTaskRepository notificationTaskRepository;

    // Внедряем зависимости через конструктор
    public NotificationScheduler(TelegramBot telegramBot, NotificationTaskRepository notificationTaskRepository) {
        this.telegramBot = telegramBot;
        this.notificationTaskRepository = notificationTaskRepository;
    }

     @Scheduled(cron = "0 0/1 * * * *")
    public void sendScheduledNotifications() {
        // 1. Получаем текущее время и обрезаем его до минут (секунды и наносекунды становятся 00)
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        // 2. Ищем в БД задачи, которые еще не отправлены и время которых равно текущему обрезанному времени
         List<NotificationTask> tasksToSend = notificationTaskRepository.findByIsSentFalseAndNotificationTime(now);

        if (tasksToSend.isEmpty()) {
            return; // Если задач нет, просто выходим из метода
        }

        log.info("Найдено {} задач для отправки на время {}", tasksToSend.size(), now);

        // 3. Проходимся циклом по всем найденным задачам
            for (NotificationTask task : tasksToSend) {
            try {
                // Формируем сообщение
                          SendMessage message = new SendMessage(task.getChatId(), task.getMessageText());

                // Отправляем сообщение
                SendResponse response = telegramBot.execute(message);

                // Проверяем успешность отправки (по шпаргалке)
                if (response.isOk()) {
                    // Если отправили успешно, помечаем задачу как отправленную
                    task.setIsSent(true);
                    notificationTaskRepository.save(task);
                    log.info("Успешно отправлено напоминание для задачи ID: {}", task.getId());
                } else {
                    log.error("Ошибка при отправке задачи ID: {}. Описание: {}", task.getId(), response.description());
                }
            } catch (Exception e) {
                log.error("Исключение при обработке задачи ID: {}", task.getId(), e);
            }
        }
    }
}