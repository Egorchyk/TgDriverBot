package com.example.tgdriverbot.repository;

import com.example.tgdriverbot.model.RoutePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoutePointRepository extends JpaRepository<RoutePoint, Long> {
    List<RoutePoint> findAllByTgUser_ChatId(Long userId);
}
