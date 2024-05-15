package com.example.tgdriverbot.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RoutePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    private String pointName;

    private String address;

    @ManyToOne
    private DailyRoute dailyRoute;

    @ManyToOne
    private TgUser tgUser;

    @Override
    public String toString() {
        return "\n\nИмя:\n" + pointName +
                "\n\nАдрес:\n" + Objects.requireNonNullElse(address, "отсутствует");
    }
}