package com.liushukov.cloud_file;

import org.springframework.boot.SpringApplication;

public class TestCloudFileApplication {

	public static void main(String[] args) {
		SpringApplication.from(CloudFileApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
