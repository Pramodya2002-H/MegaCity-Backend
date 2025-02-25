package com.System.MegaCity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MegaCityApplication {

	public static void main(String[] args) {
		SpringApplication.run(MegaCityApplication.class, args);
	}

}
