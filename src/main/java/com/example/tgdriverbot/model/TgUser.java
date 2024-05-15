package com.example.tgdriverbot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class TgUser {

    @Id
    private Long chatId;
    private String firstName;
    private String lastName;
    private String userName;

    @OneToMany
    private List<RouteList> routeLists;

    @OneToMany
    private List<RoutePoint> routePoints;

    @OneToMany
    private List<DailyRoute> dailyRoutes;

}
