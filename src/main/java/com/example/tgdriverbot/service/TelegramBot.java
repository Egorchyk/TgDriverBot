package com.example.tgdriverbot.service;

import com.example.tgdriverbot.config.BotConfig;
import com.example.tgdriverbot.model.RoutePoint;
import com.example.tgdriverbot.repository.DailyRouteRepository;
import com.example.tgdriverbot.repository.RouteListRepository;
import com.example.tgdriverbot.repository.RoutePointRepository;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final DailyRouteRepository dailyRouteRepository;
    private final RoutePointRepository routePointRepository;
    private final RouteListRepository routeListRepository;

    private final BotConfig config;

    public TelegramBot(DailyRouteRepository dailyRouteRepository, RoutePointRepository routePointRepository, RouteListRepository routeListRepository, BotConfig config) {
        this.dailyRouteRepository = dailyRouteRepository;
        this.routePointRepository = routePointRepository;
        this.routeListRepository = routeListRepository;
        this.config = config;

        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Старт"));
        listOfCommands.add(new BotCommand("/addroute", "Добавить поездку"));
        listOfCommands.add(new BotCommand("/points", "Точки"));
        listOfCommands.add(new BotCommand("/help", "Помощь по командам"));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    private int lastMessageId = -1;

    private final Map<Long, String> awaitingResponse = new HashMap<>();
    RoutePoint routePoint = new RoutePoint();

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String callBack = String.valueOf(update.getCallbackQuery());

            String currentStep = awaitingResponse.get(chatId);
            if (currentStep != null && !messageText.startsWith("/")) {
                switch (currentStep) {
                    case "pointName":
                        routePoint.setPointName(messageText);
                        sendMessage(chatId, "Вы добавили имя: " + messageText);
                        if (routePoint.getAddress() == null) {
                            awaitingResponse.put(chatId, "address");
                            sendMessage(chatId, "Введите адрес маршрута");
                        } else {
                            routePointRepository.save(routePoint);
                            routePoint = new RoutePoint();
                        }
                        break;
                    case "address":
                        routePoint.setAddress(messageText);
                        awaitingResponse.remove(chatId);
                        sendMessage(chatId, "Вы добавили маршрут: " + messageText);
                        sendMessage(chatId, "Вы добавили в бд " + routePoint.getPointName() + " " + routePoint.getAddress());
                        routePointRepository.save(routePoint);
                        routePoint = new RoutePoint();
                        break;
                }
            } else if (currentStep != null && messageText.startsWith("/") && routePoint.getPointName() != null) {
                routePointRepository.save(routePoint);
                routePoint = new RoutePoint();
                sendMessage(chatId, "вы сохранили в бд без адреса " + routePoint.getPointName());
            }

            switch (messageText) {

                case "/start":
                    //startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;

                case "/addroute":
                    addRoute(chatId);
                    break;

                case "/points":
                    getPoints(chatId);
                    break;

                case "/enddailyroute":

                    break;

                case "/help":
                    sendHelpMessage(chatId);
                    break;

                default:
                    sendMessage(chatId, "Йоу бро, ты мне не знаком!?!");
                    break;
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            String[] callbackParts = callbackData.split(";");
            sendMessage(chatId, "this callback" + callbackParts[0]);

            String action = callbackParts[0];
            String subAction = (callbackParts.length > 1) ? callbackParts[1] : null;
            long entityId = (callbackParts.length > 2) ? Long.parseLong(callbackParts[2]) : 0L;

            if (action.equals("routePoint")) {
                switch (subAction) {
                    case "snowRoute":
                        snowPoints(chatId, entityId);
                        sendMessage(chatId, "this is dva");
                        break;
                    case "createPoint":
                        sendMessage(chatId, "this callBack");
                        addPoint(chatId);
                        deleteMessage(chatId);
                        break;
                    case "editPointName":
                        routePoint = routePointRepository.findById(entityId).get();
                        awaitingResponse.put(chatId, "pointName");
                        addPoint(chatId);
                        sendMessage(chatId, "editPoint");
                        break;
                    case "editPointAddress":
                        routePoint = routePointRepository.findById(entityId).get();
                        awaitingResponse.put(chatId, "address");
                        sendMessage(chatId, "editPointAddress");
                        sendMessage(chatId, "Введите адрес маршрута");
                        break;
                    case "deletePoint":
                        routePointRepository.deleteById(entityId);
                        deleteMessage(chatId);
                        getPoints(chatId);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void snowPoints(long chatId, long entityId) {
        RoutePoint point = routePointRepository.findById(entityId).get();
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton editNameButton = InlineKeyboardButton.builder()
                .text("Изменить название")
                .callbackData("routePoint;editPointName;" + entityId)
                .build();

        InlineKeyboardButton editAddressButton = InlineKeyboardButton.builder()
                .text("Изменить адрес")
                .callbackData("routePoint;editPointAddress;" + entityId)
                .build();

        InlineKeyboardButton deleteButton = InlineKeyboardButton.builder()
                .text("Удалить")
                .callbackData("routePoint;deletePoint;" + entityId)
                .build();

        keyboardMarkup.setKeyboard(List.of(List.of(editNameButton, editAddressButton), List.of(deleteButton)));

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(point.getPointName() + "\n" + point.getAddress())
                .replyMarkup(keyboardMarkup)
                .build();
        executeMessage(sendMessage);
    }

    private void addRoute(long chatId) {
    }

    private void addPoint(long chatId) {
        sendMessage(chatId, "Введите имя маршрута:");
        awaitingResponse.put(chatId, "pointName");
    }

    private void getPoints(Long chatId) {
        List<RoutePoint> routePoints = routePointRepository.findAll();

        List<String> textList = new ArrayList<>();
        List<String> callBackDataList = new ArrayList<>();

        for (RoutePoint point : routePoints) {
            textList.add(point.getPointName());
            callBackDataList.add("routePoint;snowRoute;" + point.getId());
        }

        List<List<InlineKeyboardButton>> rows = buildInlineKeyboard(textList, callBackDataList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        rows.add(addInlineButton("Добавить новую точку", "routePoint;createPoint;"));

        inlineKeyboardMarkup.setKeyboard(rows);

        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), "Список точек");
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);

        executeMessage(sendMessage);

    }

    private void sendHelpMessage(long chatId) {
        sendMessage(chatId, "itsWorking");
    }

    private void sendMessage(long chatId, String text) {
        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteMessage(long chatId) {
        DeleteMessage deleteMessage = new DeleteMessage(String.valueOf(chatId), lastMessageId);
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void executeMessage(SendMessage sendMessage) {
        try {
            Message sentMessage = execute(sendMessage);
            lastMessageId = sentMessage.getMessageId();
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public List<List<InlineKeyboardButton>> buildInlineKeyboard(List<String> buttonTexts, List<String> callbackData, int buttonsPerRow) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (int i = 0; i < buttonTexts.size(); i += buttonsPerRow) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (int j = i; j < i + buttonsPerRow && j < buttonTexts.size(); j++) {
                String buttonText = buttonTexts.get(j);
                String callback = callbackData.get(j);
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(buttonText);
                button.setCallbackData(callback);
                row.add(button);
            }
            rows.add(row);
        }
        return rows;
    }

    private List<InlineKeyboardButton> addInlineButton(String text, String callBackData) {
        List<InlineKeyboardButton> addRow = new ArrayList<>();
        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText(text);
        addButton.setCallbackData(callBackData);
        addRow.add(addButton);
        return addRow;
    }

    private InlineKeyboardButton addInlineButton1(String text, String callBackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callBackData)
                .build();
    }
}
