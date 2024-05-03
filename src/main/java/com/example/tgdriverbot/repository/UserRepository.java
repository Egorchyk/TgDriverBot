package com.example.tgdriverbot.repository;

import com.example.tgdriverbot.model.TgUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UserRepository extends JpaRepository<TgUser, Long> {
}
