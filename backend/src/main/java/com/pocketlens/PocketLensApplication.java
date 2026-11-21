package com.pocketlens;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point of the PocketLens REST API.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
@EnableScheduling
public class PocketLensApplication {

    public static void main(String[] args) {
        SpringApplication.run(PocketLensApplication.class, args);
    }
}
