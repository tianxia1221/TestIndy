package com.dxc.cred.service;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.hyperledger.indy.sdk.IndyException;
import org.json.JSONException;

public interface RabbitMQService {

	public void init() throws IOException, TimeoutException, InterruptedException, ExecutionException, IndyException, JSONException;

	public void destroy() throws IOException, TimeoutException;

}
