package com.example.ecommerse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EcommerseApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcommerseApplication.class, args);
	}

}
