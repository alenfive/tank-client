package com.source3g.tankclient;

import com.source3g.tankclient.service.MapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@Slf4j
public class TankClientApplication implements CommandLineRunner{

	@Value("${server.port}")
	private String severPort;

	@Autowired
	private RestTemplate restTemplate;


	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}

	public static void main(String[] args) {

		SpringApplication.run(TankClientApplication.class, args);
	}

	@Autowired
	private MapService mapService;

	@Override
	public void run(String... strings) {
		restTemplate.getForEntity("http://localhost:"+severPort+"//player/systemInit",null);
	}
}
