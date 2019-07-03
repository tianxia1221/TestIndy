package com.dxc.cred.api;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperledger.indy.sdk.IndyException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.dxc.cred.domain.CredentialInfo;

import com.dxc.cred.service.CredentialInfoServiceImpl;
import com.dxc.cred.service.IndyDxcIssuerRevocServiceImpl;
import com.dxc.cred.service.ProverInfoServiceImpl;

@RestController
@EnableAutoConfiguration
@RequestMapping(value = "/api", produces = "application/json")
public class RevocationController {
    @Autowired
    private ProverInfoServiceImpl proverInfoService;
	@Autowired
	private CredentialInfoServiceImpl credentialInfoService;	
	
	@Autowired
	private IndyDxcIssuerRevocServiceImpl indyDxcIssuerRevocService;
	
    static Logger logger = LoggerFactory.getLogger(RevocationController.class);
    
    @RequestMapping(value = "/revokedcredentials", method = RequestMethod.GET)
    public void revokeCredential(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
       
    	String userName = httpRequest.getParameter("username");
        if (null == userName || userName.isEmpty()) {
            logger.error("username can not be empty");
            httpResponse.setStatus(HttpStatus.BAD_REQUEST.value());
            return;
        }
        
        CredentialInfo credInfo = credentialInfoService.findCredentialInfoByEmail(userName);
        String credRevId = credInfo.getCredRegId();
        String credStr = credInfo.getCredential();
		JSONObject credJson = new JSONObject(credStr);
        String revRegId = credJson.getString("rev_reg_id");
        
        indyDxcIssuerRevocService.revokeCredential(revRegId, credRevId);
        credentialInfoService.deleteByEmail(userName);
    }
    
    @RequestMapping(value = "/proof", method = RequestMethod.GET)
    public void verifyProof(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
    	indyDxcIssuerRevocService.verifyProof();
    }
    
}
