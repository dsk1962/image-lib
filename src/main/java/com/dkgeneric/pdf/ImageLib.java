package com.dkgeneric.pdf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication( exclude = { DataSourceAutoConfiguration.class })
@Slf4j
public class ImageLib {

	public static void main(String[] args) {
		SpringApplication.run(ImageLib.class, args);
	}
}
