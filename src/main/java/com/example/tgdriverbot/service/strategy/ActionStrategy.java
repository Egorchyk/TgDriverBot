package com.example.tgdriverbot.service.strategy;

import com.example.tgdriverbot.service.TelegramBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface ActionStrategy {
    void execute(Update update, TelegramBot bot, long chat_id);
}
