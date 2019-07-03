package com.dxc.cred.utils;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class ConnectionUtils {
	public static Connection getConnection() throws IOException, TimeoutException {
		ConnectionFactory factory = new ConnectionFactory();

		factory.setHost("prop-devops.dxctech.tk");

		factory.setPort(5672);

		return factory.newConnection();
	}
}
