package com.example.tgdriverbot.repository;//package com.example.driverbot.repository;

import com.example.tgdriverbot.model.RoutePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoutePointRepository extends JpaRepository<RoutePoint, Long> {
    @Override
    void deleteById(Long routePointId);
}
