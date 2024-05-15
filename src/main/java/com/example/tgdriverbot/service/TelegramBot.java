package com.example.tgdriverbot.service;

import com.example.tgdriverbot.config.BotConfig;
import com.example.tgdriverbot.repository.DailyRouteRepository;
import com.example.tgdriverbot.repository.RouteListRepository;
import com.example.tgdriverbot.repository.RoutePointRepository;
import com.example.tgdriverbot.service.dbservice.TgUserService;
import com.example.tgdriverbot.service.strategy.RegistrationStrategy;
import com.example.tgdriverbot.service.strategy.RoutePointActionHandler;
import com.example.tgdriverbot.service.strategy.ActionStrategy;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Getter
@Setter
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final TgUserService tgUserService;
    private final DailyRouteRepository dailyRouteRepository;
    private final RoutePointRepository routePointRepository;
    private final RouteListRepository routeListRepository;

    private final BotConfig config;


    public TelegramBot(TgUserService tgUserService, DailyRouteRepository dailyRouteRepository, RoutePointRepository routePointRepository, RouteListRepository routeListRepository, BotConfig config) {
        this.tgUserService = tgUserService;
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

    private static final int MAX_STACK_SIZE = 10;
    private List<Integer> lastMessageIdList = new ArrayList<>();
    private int userMessageId;

    private ActionStrategy actionStrategy;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText() && update.getMessage().getText().startsWith("/")) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String callBack = String.valueOf(update.getCallbackQuery());

            switch (messageText) {
                case "/start":
                    actionStrategy = new RegistrationStrategy(tgUserService);
                    actionStrategy.execute(update, this, chatId);
                    //startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/addroute":
                    break;
                case "/points":
                    actionStrategy = new RoutePointActionHandler(tgUserService);
                    actionStrategy.execute(update, this, chatId);
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

        } else if (actionStrategy != null) {
            long chatId = -1;
            if (update.hasMessage()) {
                chatId = update.getMessage().getChatId();
            } else if (update.hasCallbackQuery()) {
                chatId = update.getCallbackQuery().getMessage().getChatId();
            }
            actionStrategy.execute(update, this, chatId);
        }
    }

    private void sendHelpMessage(long chatId) {
        sendMessage(chatId, "itsWorking");
    }

    public void sendMessage(long chatId, String text) {
        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), text);
        try {
            lastMessageIdList.add(execute(sendMessage).getMessageId());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void editMessage(SendMessage sendMessage, int countMessageForEdit) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(sendMessage.getChatId());
        if (lastMessageIdList.size() > countMessageForEdit) {
            int lastMessageId = lastMessageIdList.get(lastMessageIdList.size() - 1 - countMessageForEdit);
            editMessageText.setMessageId(lastMessageId);
        }
        editMessageText.setText(sendMessage.getText());
        lastMessageIdList.add(editMessageText.getMessageId());

        if (sendMessage.getReplyMarkup() != null) {
            editMessageText.setReplyMarkup((InlineKeyboardMarkup) sendMessage.getReplyMarkup());
            System.out.println("KLAVA ESTb");
        }

        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteMessage(long chatId, int countMessageForDelete) {
        DeleteMessage deleteMessage = null;
        if (lastMessageIdList.size() >= countMessageForDelete) {
            int sizeList = lastMessageIdList.size() - 1;
            deleteMessage = new DeleteMessage(String.valueOf(chatId), lastMessageIdList.remove(sizeList - countMessageForDelete));
        }

        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteLastUserMessage(long chatId) {
        try {
            execute(DeleteMessage.builder().chatId(chatId).messageId(userMessageId).build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void executeMessage(SendMessage sendMessage) {
        try {
            Message sentMessage = execute(sendMessage);
            if (lastMessageIdList.size() >= MAX_STACK_SIZE) {
                lastMessageIdList.remove(lastMessageIdList.size() - 1);
            }
            lastMessageIdList.add(sentMessage.getMessageId());
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

    public List<InlineKeyboardButton> addInlineButton(String text, String callBackData) {
        List<InlineKeyboardButton> addRow = new ArrayList<>();
        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText(text);
        addButton.setCallbackData(callBackData);
        addRow.add(addButton);
        return addRow;
    }
}
