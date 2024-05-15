package com.example.tgdriverbot.service.dbservice;

import com.example.tgdriverbot.model.RoutePoint;
import com.example.tgdriverbot.model.TgUser;
import com.example.tgdriverbot.repository.RoutePointRepository;
import com.example.tgdriverbot.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TgUserService {

    private final UserRepository userRepository;
    private final RoutePointRepository routePointRepository;

    public TgUserService(UserRepository userRepository, RoutePointRepository routePointRepository) {
        this.userRepository = userRepository;
        this.routePointRepository = routePointRepository;
    }

    public void save(TgUser user) {
        userRepository.save(user);
    }

    public void saveRoutePoint(TgUser tgUser, RoutePoint routePoint) {
        routePoint.setTgUser(tgUser);
        routePointRepository.save(routePoint);
    }

    public TgUser findByUserId(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public List<TgUser> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteRoutePointById(long id) {
        routePointRepository.deleteById(id);
    }

    public RoutePoint findRoutePointById(Long id) {
        return routePointRepository.findById(id).orElse(null);
    }

    public List<RoutePoint> getAllRoutePoints(TgUser tgUser) {
        return routePointRepository.findAllByTgUser_ChatId(tgUser.getChatId());
    }
}
