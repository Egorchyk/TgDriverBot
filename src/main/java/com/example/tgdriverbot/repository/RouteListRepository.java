package com.example.tgdriverbot.repository;

import com.example.tgdriverbot.model.RouteList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteListRepository extends JpaRepository<RouteList, Long> {
    List<RouteList> findAllByMapLocationNot(String mapLocation);
}
