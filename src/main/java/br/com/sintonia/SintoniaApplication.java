package br.com.sintonia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SintoniaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SintoniaApplication.class, args);
    }

}
