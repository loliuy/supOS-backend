package com.supos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@SpringBootApplication
@ComponentScan(basePackages = {"com.supos"})
// 自测：-Dspring.profiles.active=local
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class);
    }

    @RestController
    public class HealthCheckController {
        @GetMapping(path = "/", produces = "application/json;charset=UTF-8")
        public String home() {
            return "{\"msg\":\"ok\",\"time\":\"" + new Date() + "\"}";
        }
    }

}
