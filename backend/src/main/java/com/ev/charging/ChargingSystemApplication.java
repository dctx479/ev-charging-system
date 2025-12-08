package com.ev.charging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * EV Charging Station Management System - Main Application
 *
 * @author EV Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class ChargingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChargingSystemApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("EV Charging System Started Successfully!");
        System.out.println("API Base URL: http://localhost:8080/api");
        System.out.println("========================================\n");
    }
}
