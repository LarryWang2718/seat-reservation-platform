package com.project.seat_reserve;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SeatReserveApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeatReserveApplication.class, args);
    }

}
