package com.dxc.cred.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "com.dxc.cred.api", "com.dxc.cred.service"} )
public class CredentialApplication {
	@Autowired
	RabbitMQServiceImpl rabbitMQService;
	static Logger logger = LoggerFactory.getLogger(CredentialApplication.class);
	
	public static void main(String[] args) {	
		SpringApplication.run(CredentialApplication.class, args);
		
	}

}
