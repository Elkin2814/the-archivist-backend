package com.thearchivist.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TheArchivistApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TheArchivistApiApplication.class, args);
	}

}
