package com.example.tgdriverbot.service.strategy;

import com.example.tgdriverbot.model.TgUser;
import com.example.tgdriverbot.service.TelegramBot;
import com.example.tgdriverbot.service.dbservice.TgUserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class RegistrationStrategy implements ActionStrategy {

    private final TgUserService tgUserService;

    public RegistrationStrategy(TgUserService tgUserService) {
        this.tgUserService = tgUserService;
    }

    @Override
    public void execute(Update update, TelegramBot bot, long chatId) {
        long userId = update.getMessage().getFrom().getId();
        String firstName = update.getMessage().getFrom().getFirstName();
        String lastName = update.getMessage().getFrom().getLastName();
        String userName = update.getMessage().getFrom().getUserName();

        TgUser existingUser = tgUserService.findByUserId(userId);

        if (existingUser == null) {
            TgUser newUser = new TgUser();
            newUser.setChatId(userId);
            newUser.setUserName(userName);
            newUser.setLastName(lastName);
            newUser.setFirstName(firstName);

            tgUserService.save(newUser);

            SendMessage message = new SendMessage();
            message.setChatId(userId);
            message.setText("Вы успешно зарегистрированы! \n\n");
            bot.executeMessage(message);
        } else {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Вы уже зарегистрированы!");
            bot.executeMessage(message);
        }
    }
}
