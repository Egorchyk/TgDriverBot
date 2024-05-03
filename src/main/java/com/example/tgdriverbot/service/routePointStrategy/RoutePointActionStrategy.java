package com.example.tgdriverbot.service.routePointStrategy;

import com.example.tgdriverbot.service.TelegramBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface RoutePointActionStrategy {
    void execute(Update update, TelegramBot bot, long chat_id);
}
