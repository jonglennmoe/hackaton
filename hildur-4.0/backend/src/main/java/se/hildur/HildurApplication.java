package se.hildur;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point. Spring Boot scans this package and its sub-packages for
 * controllers, services and repositories and wires them together.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class HildurApplication {

    public static void main(String[] args) {
        SpringApplication.run(HildurApplication.class, args);
    }
}
