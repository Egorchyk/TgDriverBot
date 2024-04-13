package com.example.tgdriverbot.repository;//package com.example.driverbot.repository;

import com.example.tgdriverbot.model.DailyRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyRouteRepository extends JpaRepository<DailyRoute, Long> {
}
