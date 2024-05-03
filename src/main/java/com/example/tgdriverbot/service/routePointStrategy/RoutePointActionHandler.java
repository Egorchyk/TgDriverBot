package com.example.tgdriverbot.service.routePointStrategy;

import com.example.tgdriverbot.model.RoutePoint;
import com.example.tgdriverbot.service.TelegramBot;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

@Component
public class RoutePointActionHandler implements RoutePointActionStrategy {

    private static final String CREATE_ROUTE_POINT_STEP = "createRoutePoint";
    private static final String POINT_NAME_STEP = "pointName";
    private static final String POINT_ADDRESS_STEP = "pointAddress";
    private static final String DELETE_POINT_STEP = "deletePoint";

    private TelegramBot bot;
    private String messageText;

    @Override
    public void execute(Update update, TelegramBot bot, long chatId) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            this.messageText = update.getMessage().getText();
            this.bot = bot;
        }

        if (bot.getAwaitingResponse().get(chatId) != null) {
            bot.deleteMessage(chatId, 0);
            addPoints(chatId);
        }

        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            String[] callbackParts = callbackData.split(";");

            String action = callbackParts[0];
            long entityId = (callbackParts.length > 1) ? Long.parseLong(callbackParts[1]) : -1;
            bot.deleteMessage(chatId, 0);

            switch (action) {
                case CREATE_ROUTE_POINT_STEP:
                    bot.setAwaitingResponse(Collections.singletonMap(chatId, "createRoutePoint"));
                    messageText = "";
                    addPoints(chatId);
                    break;
                case POINT_NAME_STEP:
                    bot.setAwaitingResponse(Collections.singletonMap(chatId, "pointName"));
                    bot.setRoutePoint(bot.getRoutePointRepository().findById(entityId).get());
                    bot.sendMessage(chatId, "Введите имя:");
                    break;
                case POINT_ADDRESS_STEP:
                    bot.setAwaitingResponse(Collections.singletonMap(chatId, "pointAddress"));
                    bot.setRoutePoint(bot.getRoutePointRepository().findById(entityId).get());
                    bot.sendMessage(chatId, "Введите адресс:");
                    break;
                case DELETE_POINT_STEP:
                    bot.sendMessage(chatId, "Вы удалили точку:" + bot.getRoutePointRepository().findById(entityId).get().getPointName());
                    bot.getRoutePointRepository().deleteById(entityId);
                    bot.deleteMessage(chatId, 1);
                    getPoints(chatId);
                    break;
                case "snowRoute":
                    snowPoints(chatId, entityId);
                    break;
            }
        } else if (messageText.startsWith("/")) {
            getPoints(chatId);
        }
    }

    private void addPoints(long chatId) {
        String currentStep = bot.getAwaitingResponse().get(chatId);

        if (currentStep != null && !messageText.startsWith("/")) {
            switch (currentStep) {
                case CREATE_ROUTE_POINT_STEP:
                    handleCreateRoutePointStep(chatId);
                    break;
                case POINT_ADDRESS_STEP:
                    handlePointAddressStep(chatId);
                    break;
                case POINT_NAME_STEP:
                    handlePointNameStep(chatId);
                    break;
            }
        } else if (currentStep != null && messageText.startsWith("/") && bot.getRoutePoint().getPointName() != null) {
            saveRoutePointWithoutAddress(chatId);
        }
    }

    private void handleCreateRoutePointStep(long chatId) {
        bot.sendMessage(chatId, "Введите имя точки:");
        bot.setAwaitingResponse(Collections.singletonMap(chatId, POINT_NAME_STEP));
    }

    private void handlePointAddressStep(long chatId) {
        bot.getRoutePoint().setAddress(messageText);
        bot.setAwaitingResponse(new HashMap<>());
        if (bot.getRoutePoint().getId() == null) {
            handlePointAddressNotInDatabase(chatId);
        } else {
            handlePointAddressUpdated(chatId);
        }
    }

    private void handlePointAddressNotInDatabase(long chatId) {
        bot.sendMessage(chatId, "Вы добавили адресс точки: " + messageText);
        bot.sendMessage(chatId, "Вы добавили в бд " + bot.getRoutePoint().getPointName() + " " + bot.getRoutePoint().getAddress());
        bot.getRoutePointRepository().save(bot.getRoutePoint());
        bot.setRoutePoint(new RoutePoint());
    }

    private void handlePointAddressUpdated(long chatId) {
        bot.sendMessage(chatId, "Вы обновили точку: " + messageText);
        bot.sendMessage(chatId, "Вы обновили точку в бд " + bot.getRoutePoint().getPointName() + " " + bot.getRoutePoint().getAddress());
        bot.getRoutePointRepository().save(bot.getRoutePoint());
        bot.setRoutePoint(new RoutePoint());
    }

    private void handlePointNameStep(long chatId) {
        bot.getRoutePoint().setPointName(messageText);
        if (bot.getRoutePoint().getId() == null) {
            handlePointNameNotInDatabase(chatId);
        } else {
            handlePointNameUpdated(chatId);
        }
    }

    private void handlePointNameNotInDatabase(long chatId) {
        bot.sendMessage(chatId, "Вы добавили имя: " + messageText);
        if (bot.getRoutePoint().getAddress() == null) {
            bot.setAwaitingResponse(Collections.singletonMap(chatId, POINT_ADDRESS_STEP));
            bot.sendMessage(chatId, "Введите адрес точки:");
        } else {
            bot.getRoutePointRepository().save(bot.getRoutePoint());
            bot.setRoutePoint(new RoutePoint());
        }
    }

    private void handlePointNameUpdated(long chatId) {
        bot.sendMessage(chatId, "Вы обновили имя: " + bot.getRoutePoint().getPointName());
        if (bot.getRoutePoint().getAddress() == null) {
            bot.setAwaitingResponse(Collections.singletonMap(chatId, POINT_ADDRESS_STEP));
            bot.sendMessage(chatId, "Введите адрес точки:");
        } else {
            bot.getRoutePointRepository().save(bot.getRoutePoint());
            bot.setAwaitingResponse(new HashMap<>());
            bot.setRoutePoint(new RoutePoint());
        }
    }

    private void saveRoutePointWithoutAddress(long chatId) {
        bot.getRoutePointRepository().save(bot.getRoutePoint());
        bot.sendMessage(chatId, "вы сохранили в бд без адреса " + bot.getRoutePoint().getPointName());
        bot.setRoutePoint(new RoutePoint());
    }


    private void getPoints(long chatId) {
        List<RoutePoint> routePoints = bot.getRoutePointRepository().findAll();

        List<String> textList = new ArrayList<>();
        List<String> callBackDataList = new ArrayList<>();

        for (RoutePoint point : routePoints) {
            textList.add(point.getPointName());
            callBackDataList.add("snowRoute;" + point.getId());
        }

        List<List<InlineKeyboardButton>> rows = bot.buildInlineKeyboard(textList, callBackDataList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        rows.add(bot.addInlineButton("Добавить новую точку", CREATE_ROUTE_POINT_STEP));

        inlineKeyboardMarkup.setKeyboard(rows);

        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), "Список точек");
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);

        bot.executeMessage(sendMessage);
    }

    private void snowPoints(long chatId, long entityId) {
        RoutePoint point = bot.getRoutePointRepository().findById(entityId).get();
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton editNameButton = InlineKeyboardButton.builder()
                .text("Изменить название")
                .callbackData(POINT_NAME_STEP + ";" + entityId)
                .build();

        InlineKeyboardButton editAddressButton = InlineKeyboardButton.builder()
                .text("Изменить адрес")
                .callbackData(POINT_ADDRESS_STEP + ";" + entityId)
                .build();

        InlineKeyboardButton deleteButton = InlineKeyboardButton.builder()
                .text("Удалить")
                .callbackData(DELETE_POINT_STEP + ";" + entityId)
                .build();

        keyboardMarkup.setKeyboard(List.of(List.of(editNameButton, editAddressButton), List.of(deleteButton)));

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(point.getPointName() + "\n" + point.getAddress())
                .replyMarkup(keyboardMarkup)
                .build();
        bot.executeMessage(sendMessage);
        bot.deleteMessage(chatId, 1);
    }
}

