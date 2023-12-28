package com.osstelecom.db.inventory.manager.client.netcompass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NetcompassClientApp {

	public static void main(String[] args) {
		System.exit(SpringApplication.exit(SpringApplication.run(NetcompassClientApp.class, args)));
	}

}
