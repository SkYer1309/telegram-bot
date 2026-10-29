package pro.sky.telegrambot.configuration;

import com.pengrad.telegrambot.TelegramBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelegramBotConfiguration {

    @Value("${telegram.bot.token}")
    private String token;

    @Bean
    public TelegramBot telegramBot() {
        // ВАЖНО: Мы просто создаем объект бота.
        // Никаких bot.execute() здесь быть не должно!
        return new TelegramBot(token);
    }
}