package com.ev.charging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * EV Charging Station Management System - Main Application
 *
 * @author EV Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@Slf4j
public class ChargingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChargingSystemApplication.class, args);
        log.info("========================================");
        log.info("EV Charging System Started Successfully!");
        log.info("API Base URL: http://localhost:8080/api");
        log.info("========================================");
    }
}
