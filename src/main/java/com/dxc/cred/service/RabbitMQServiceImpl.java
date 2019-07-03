package com.dxc.cred.service;

import com.dxc.cred.utils.ConnectionUtils;
import com.dxc.cred.err.BaseConfigurationException;
import com.rabbitmq.client.*;
import org.hyperledger.indy.sdk.IndyException;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;
@Service
public class RabbitMQServiceImpl implements RabbitMQService{
    
	@Autowired
    private IndyDxcIssuerRevocServiceImpl indyDxcIssuerService;
    private final static String CRED_OFFER_REQUEST_QUEUE = "credentialOfferRequest";
    private final static String CRED_OFFER_RESPONSE_QUEUE = "credentialOfferResponse_";
    private final static String CRED_REQUEST_QUEUE = "credentialRequest";
    private final static String CRED_RESPONSE_QUEUE = "credentialResponse_";
    
	private static final String credOfferReqTest = "{\n"
			 + "  \"type\": \"credentialOfferResponse\",\n"
			 + "   \"value\": { \n"
			+ " 	    credentialName: \"transcript\",\n"
			+ " 	    email: \"xia.tian@dxc.com\",\n"
				+ "     token: \"148290\"\n"
				+ " 	  },\n"
				+ "     src: \"13579\"\n"
				+ " 		}";
	private static final String credReqTest = "{\n"
			 + "  \"type\": \"credentialOfferResponse\",\n"
			 + "   \"value\": { \n"
			+ " 	    email: \"xia.tian@dxc.com\",\n"
			+ " 	    credentialOffer: \"credentialOffer\",\n"
				+ "     credentialReq: \"credentialReq\"\n"
				+ " 	  },\n"
				+ "     src: \"24680\"\n"
				+ " 		}";
    
    private static Connection connection = null;
    private static Channel channel = null;
    
    @PostConstruct
    @Override
    public void init() throws IOException, TimeoutException, InterruptedException, ExecutionException, IndyException, JSONException{
    	boolean ret = initQueue();
    	if(!ret) {
    		throw new BaseConfigurationException("initilizaing queue failed");
    	}
    	ret = indyDxcIssuerService.init();
    	if(!ret) {
    		throw new BaseConfigurationException("initilizaing indyDxcIssuerService failed");
    	}
    }
    
    @Override
    public void destroy() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }
    
//    public boolean test1() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException, TimeoutException {    	
//    	indyService.test();
//    	//indyService.init();
//    //	publishMsg(CRED_OFFER_REQUEST_QUEUE, credOfferReqTest);
//    //	publishMsg(CRED_REQUEST_QUEUE, credReqTest);    	
//    	return true;
//    }
    
    private boolean initQueue() throws IOException, TimeoutException {
    	Connection connection = ConnectionUtils.getConnection();
    	channel = connection.createChannel();
    	channel.queueDeclare(CRED_OFFER_REQUEST_QUEUE, false, false, false, null);
    	channel.queueDeclare(CRED_REQUEST_QUEUE, false, false, false, null);
    	
	 Consumer consumerOfferReq = new DefaultConsumer(channel) {
	      @Override
	      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
	          throws IOException {
	        String message = new String(body, "UTF-8");
	        System.out.println(" [x] Received from Offer Request'" + message + "'");
	        String response = "";
			try {
				response = indyDxcIssuerService.createCredOfferResponse(message);
				
				JSONObject offerReqJson = new JSONObject(message);
		        String src = (String)offerReqJson.get("src");
		        String queue = CRED_OFFER_RESPONSE_QUEUE + src;
				channel.queueDeclare(queue, false, false, false, null);
				
				publishMsg(queue, response); 
				System.out.println(" [x] Received from Offer Response'" + response + "'");
			} catch (JSONException e) {
				e.printStackTrace();
			}	          
	      }
	    };
	 channel.basicConsume(CRED_OFFER_REQUEST_QUEUE, true, consumerOfferReq);
	 
	 Consumer consumerCredrReq = new DefaultConsumer(channel) {
	      @Override
	      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
	          throws IOException {
	        String message = new String(body, "UTF-8");
	        System.out.println(" [x] Received from Credential Request'" + message + "'");
	        String response = "";
			try {
				response = indyDxcIssuerService.createCredResponse(message);
				
		        JSONObject credReqJson = new JSONObject(message);
		        String src = (String)credReqJson.get("src");
		        String queue = CRED_RESPONSE_QUEUE + src;
				channel.queueDeclare(queue, false, false, false, null);
				
				publishMsg(queue, response); 				
				System.out.println(" [x] Received from Credential Response'" + response + "'");
			} catch (JSONException e) {
				e.printStackTrace();
			}
	      }
	    };
	 channel.basicConsume(CRED_REQUEST_QUEUE, true, consumerCredrReq);
    	    
    	return true;
    }
    
    private void publishMsg(String queueName, String msg) throws UnsupportedEncodingException, IOException {
    	channel.basicPublish("", queueName, null, msg.getBytes("UTF-8"));
    }
    
}
