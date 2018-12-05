package com.source3g.tankclient;

import com.source3g.tankclient.service.MapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class TankClientApplication implements CommandLineRunner{

	public static void main(String[] args) {

		SpringApplication.run(TankClientApplication.class, args);
	}

	@Autowired
	private MapService mapService;

	@Override
	public void run(String... strings) {


	}
}
