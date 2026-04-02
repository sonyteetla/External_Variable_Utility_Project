package com.externalvariable.cobolparser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.WebApplicationType;

@SpringBootApplication
public class CobolParserApplication {

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(CobolParserApplication.class);

        // 🔥 THIS IS THE FIX
        app.setWebApplicationType(WebApplicationType.NONE);

        app.run(args);
    }
}