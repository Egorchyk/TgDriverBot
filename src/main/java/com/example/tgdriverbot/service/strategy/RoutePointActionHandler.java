package com.example.tgdriverbot.service.strategy;

import com.example.tgdriverbot.model.RoutePoint;
import com.example.tgdriverbot.model.TgUser;
import com.example.tgdriverbot.service.TelegramBot;
import com.example.tgdriverbot.service.dbservice.TgUserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

@Component
public class RoutePointActionHandler implements ActionStrategy {

    private static final String CREATE_ROUTE_POINT_STEP = "createRoutePoint";
    private static final String POINT_NAME_STEP = "pointName";
    private static final String POINT_ADDRESS_STEP = "pointAddress";
    private static final String DELETE_POINT_STEP = "deletePoint";
    private static final String SHOW_ALL_POINTS = "getAllPoints";
    private static final String SHOW_ROUTE = "snowRoute";

    private TelegramBot bot;
    private String messageText;
    private final TgUserService tgUserService;
    private TgUser currentUser;
    private RoutePoint routePoint = new RoutePoint();
    private Map<Long, String> awaitingResponse = new HashMap<>();

    public RoutePointActionHandler(TgUserService tgUserService) {
        this.tgUserService = tgUserService;
    }

    @Override
    public void execute(Update update, TelegramBot bot, long userId) {

        currentUser = tgUserService.findByUserId(userId);

        this.bot = bot;
        this.messageText = (update.hasMessage() && update.getMessage().hasText()) ? update.getMessage().getText() : "";

        if (update.hasMessage()) {
            bot.setUserMessageId(update.getMessage().getMessageId());
            bot.deleteLastUserMessage(userId);
        }

        if (awaitingResponse.get(userId) != null) {
            addPoints(userId);
        }

        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            String[] callbackParts = callbackData.split(";");

            String action = callbackParts[0];
            long entityId = (callbackParts.length > 1) ? Long.parseLong(callbackParts[1]) : -1;

            switch (action) {
                case CREATE_ROUTE_POINT_STEP:
                    awaitingResponse.put(userId, "createRoutePoint");
                    messageText = "";
                    addPoints(userId);
                    break;
                case POINT_NAME_STEP:
                    awaitingResponse.put(userId, "pointName");
                    routePoint = tgUserService.findRoutePointById(entityId);
                    bot.editMessage(SendMessage.builder()
                            .chatId(userId)
                            .text("Введите название:")
                            .build(), 0);
                    break;
                case POINT_ADDRESS_STEP:
                    awaitingResponse.put(userId, "pointAddress");
                    routePoint = tgUserService.findRoutePointById(entityId);
                    bot.editMessage(SendMessage.builder()
                            .chatId(userId)
                            .text("Введите адресс:")
                            .build(), 0);
                    break;
                case DELETE_POINT_STEP:
                    deleteRoutePoint(userId, entityId);
                    break;

                case SHOW_ALL_POINTS:
                    getPoints(userId);
                    break;

                case SHOW_ROUTE:
                    snowPoint(userId, entityId);
                    break;
            }
        } else if (messageText.startsWith("/")) {
            getPoints(userId);
        }
    }

    private void addPoints(long chatId) {
        String currentStep = awaitingResponse.get(chatId);

        if (currentStep != null) {
            if (!messageText.startsWith("/")) {
                switch (currentStep) {
                    case CREATE_ROUTE_POINT_STEP:
                        handleCreateRoutePointStep(chatId);
                        break;
                    case POINT_NAME_STEP:
                        handlePointNameStep(chatId);
                        break;
                    case POINT_ADDRESS_STEP:
                        handlePointAddressStep(chatId);
                        break;
                }
            } else if (messageText.startsWith("/") && routePoint.getPointName() != null) {
                saveRoutePointWithoutAddress(chatId);
            }
        }
    }

    private void handleCreateRoutePointStep(long chatId) {
        bot.editMessage(SendMessage.builder()
                .chatId(chatId)
                .text("Введите имя пункта:")
                .build(), 0);
        awaitingResponse.put(chatId, POINT_NAME_STEP);
    }

    private void handlePointAddressStep(long chatId) {
        routePoint.setAddress(messageText);
        resetAwaitingResponse();
        if (routePoint.getId() == null) {
            handlePointAddress(chatId,"Вы сохранили новый пункт ");
        } else {
            handlePointAddress(chatId,"Вы обновили адрес пункта ");
        }
    }

    private void handlePointAddress(long chatId, String successMessage) {
        bot.editMessage(SendMessage.builder()
                .chatId(chatId)
                .text(successMessage + routePoint)
                .replyMarkup(getInlineKeyboardShowAllPoint())
                .build(), 0);
        tgUserService.saveRoutePoint(currentUser, routePoint);
        resetRoutePoint();
    }

    private void handlePointNameStep(long chatId) {
        routePoint.setPointName(messageText);
        if (routePoint.getId() == null) {
            handleCreateRoutePoint(chatId);
        } else {
            handlePointNameUpdated(chatId);
        }
    }

    private void handleCreateRoutePoint(long chatId) {
        if (messageText.isEmpty()) {
            resetAwaitingResponse();
            routePoint = null;
        } else if (routePoint.getAddress() == null) {
            awaitingResponse.put(chatId, POINT_ADDRESS_STEP);
            bot.editMessage(SendMessage.builder()
                            .chatId(chatId)
                            .text("Вы ввели пункт: " + messageText + "\n\nВведите адрес пункта:")
                            .build()
                    , 0);
        } else {
            tgUserService.saveRoutePoint(currentUser, routePoint);
            resetRoutePoint();
        }
    }

    private void handlePointNameUpdated(long chatId) {
        bot.editMessage(SendMessage.builder()
                .chatId(chatId)
                .text("Вы обновили имя пункта: " + routePoint)
                .replyMarkup(getInlineKeyboardShowAllPoint())
                .build(), 0);
        tgUserService.saveRoutePoint(currentUser, routePoint);
        if (routePoint.getAddress() == null) {
            awaitingResponse.put(chatId, POINT_ADDRESS_STEP);
            bot.editMessage(SendMessage.builder()
                    .chatId(chatId)
                    .text("Введите адрес пункта:")
                    .build(), 0);
        } else {
            tgUserService.saveRoutePoint(currentUser, routePoint);
            resetAwaitingResponse();
            resetRoutePoint();
        }
    }

    private void saveRoutePointWithoutAddress(long chatId) {
        tgUserService.saveRoutePoint(currentUser, routePoint);
        bot.editMessage(SendMessage.builder()
                .chatId(chatId)
                .text("Вы сохранили пункт без адреса: " + routePoint.toString())
                .replyMarkup(getInlineKeyboardShowAllPoint())
                .build(), 0);
        resetRoutePoint();
    }

    private void deleteRoutePoint(long chatId, long entityId) {
        bot.editMessage(SendMessage.builder()
                .chatId(chatId)
                .text("Вы удалили пункт:" + tgUserService.findRoutePointById(entityId))
                .replyMarkup(getInlineKeyboardShowAllPoint())
                .build(), 0);
        tgUserService.deleteRoutePointById(entityId);
    }

    private void getPoints(long chatId) {
        //TODO: ошибка при двойном открытии /point
        List<RoutePoint> routePoints = tgUserService.getAllRoutePoints(currentUser);

        List<String> textList = new ArrayList<>();
        List<String> callBackDataList = new ArrayList<>();

        for (RoutePoint point : routePoints) {
            textList.add(point.getPointName());
            callBackDataList.add(SHOW_ROUTE + ";" + point.getId());
        }

        List<List<InlineKeyboardButton>> rows = bot.buildInlineKeyboard(textList, callBackDataList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        rows.add(bot.addInlineButton("Добавить новый пункт", CREATE_ROUTE_POINT_STEP));

        inlineKeyboardMarkup.setKeyboard(rows);

        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), "Список точек:");
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);

        bot.editMessage(sendMessage, 0);
    }

    private void snowPoint(long chatId, long entityId) {
        RoutePoint point = tgUserService.findRoutePointById(entityId);
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

        InlineKeyboardButton getAllPointButton = InlineKeyboardButton.builder()
                .text("Все пункты")
                .callbackData(SHOW_ALL_POINTS)
                .build();

        keyboardMarkup.setKeyboard(List.of(
                List.of(editNameButton, editAddressButton), List.of(deleteButton), List.of(getAllPointButton)));
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(point.toString())
                .replyMarkup(keyboardMarkup)
                .build();
        bot.editMessage(sendMessage, 0);
    }

    private ReplyKeyboard getInlineKeyboardShowAllPoint() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(List.of(List.of(InlineKeyboardButton.builder()
                .text("Показать все пункты")
                .callbackData(SHOW_ALL_POINTS)
                .build())));
        return inlineKeyboardMarkup;
    }

    private void resetRoutePoint() {
        this.routePoint = new RoutePoint();
    }

    private void resetAwaitingResponse() {
        this.awaitingResponse = new HashMap<>();
    }

}

