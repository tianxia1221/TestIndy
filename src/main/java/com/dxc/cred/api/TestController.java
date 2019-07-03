package com.dxc.cred.api;

import com.dxc.cred.domain.CredentialInfo;
import com.dxc.cred.domain.ProverInfo;
import com.dxc.cred.service.CredentialInfoServiceImpl;
import com.dxc.cred.service.IndyDxcIssuerRevocServiceImpl;
import com.dxc.cred.service.IndyDxcIssuerServiceImpl;
import com.dxc.cred.service.IndyTrusteeServiceImpl;
import com.dxc.cred.service.ProverInfoServiceImpl;

import org.hyperledger.indy.sdk.IndyException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@EnableAutoConfiguration
public class TestController {
    @Autowired
    private ProverInfoServiceImpl proverInfoService;
    
    @Autowired
    private CredentialInfoServiceImpl credentialInfoService;
    
    
    @Autowired
    private IndyDxcIssuerRevocServiceImpl indyDxcIssuerRevocService;
    
    static Logger logger = LoggerFactory.getLogger(TestController.class);

    @Autowired 
    CredentialInfoServiceImpl credentialInfoServiceImpl1;
    
    @Autowired 
    IndyTrusteeServiceImpl indyTrusteeService;
    
    @Autowired 
    IndyDxcIssuerServiceImpl indyDxcIssuerService;
    @RequestMapping(value = "/testdb", method = RequestMethod.GET)
    public void testdb(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
    	indyDxcIssuerRevocService.testdb();	

    }
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
    
    @RequestMapping("/revoc")
    public String revoc() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
    	
    	indyTrusteeService.init();
    	indyTrusteeService.onboardingDXC();
    	
    	indyDxcIssuerRevocService.init();
    	indyDxcIssuerRevocService.testCreateSchemaCred();
    	
        return "Hello World";
    }
    
  
    @RequestMapping("/search")
    public String search() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
    	
      indyDxcIssuerService.init();
    	
        return "Hello World";
    }
    
    @RequestMapping("/trustee")
    public String onboarding() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
    	
    	indyTrusteeService.init();
    	indyTrusteeService.onboardingDXC();
    	
    	indyDxcIssuerService.init();
    	indyDxcIssuerService.testCreateSchemaCred();
    	
        return "Hello World";
    }
    
//    @RequestMapping("/credential")
//    public String credential() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
//    	CredentialInfoServiceImpl service= credentialInfoServiceImpl1;
//    	String id = "97250ddd3257880595867874871";
//    	String offer = "";
//    	for(int i=0 ; i<4000; i++) {
//    		offer += "d";
//    	}
//    	CredentialInfo credentialInfo = new CredentialInfo(id, "xia.tian@dxc.com", "123456", offer); 
////    	service.createCredentialInfo(credentialInfo);
////    	
//    	id = "420413977365518221940461";
//    	credentialInfo = service.findCredentialInfoById(id);
//    	
//
//    	credentialInfo.setCredential("credential info info");
//    	service.updateCredentialByUuid(credentialInfo);
//        return "Hello World";
//    }
    
    @RequestMapping("/indy")
    public String prover() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
        ProverInfo proverInfo = new ProverInfo(UUID.randomUUID().toString(), "tian", "xia", "xia.tian@dxc.com", "dxc");
        proverInfoService.createProverInfo(proverInfo);
        return "Hello World";
    }
    
    @RequestMapping("/list")
    public List<ProverInfo>  getProverInfoList() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {        
        return proverInfoService.getActiveInfoList();
    }
    
    @RequestMapping("/batch")
    public String batch() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
    	List<ProverInfo>  list = new ArrayList<>();
        ProverInfo proverInfo1 = new ProverInfo(UUID.randomUUID().toString(), "tian", "xia", "xia.tian@dxc.com","dxc");
        ProverInfo proverInfo2 = new ProverInfo(UUID.randomUUID().toString(), "tian", "xia", "xia.tian@dxc.com","dxc");
        list.add(proverInfo1);
        list.add(proverInfo2);
        proverInfoService.createProverInfoList(list);
        return "batch Hello World";
    }
    
    @RequestMapping("/nonce")
    public String updateNonce() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
        String id = "c8654893-91a7-4e33-93ba-4750a19c3003";
        ProverInfo info = new ProverInfo();
        info.setUuid(id);
        proverInfoService.updateNonceByUuid(info);
        return " nouce Hello World";
    }
    
    @RequestMapping("/noucebatch")
    public String updateNonceBatch() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
    	List<ProverInfo>  list = new ArrayList<>();
    	
        String id = "c8654893-91a7-4e33-93ba-4750a19c3003";
        ProverInfo info = new ProverInfo();
        info.setUuid(id);
        
        list.add(info);
        
        id = "bbb284f0-df96-4f13-91a5-1374f71a02ae";
        ProverInfo info1 = new ProverInfo();
        info.setUuid(id);
        
        list.add(info1);
    	
        proverInfoService.updateNonceByUuidList(list);
        return " nouce Hello World";
    }
    
    @RequestMapping("/deleted")
    public String deleteProverInfo() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
        String id = "ee2fb16a-9f87-447e-ba2d-c378364df135";
        proverInfoService.deleteProverInfoByUuid(id);
        return " nouce Hello World";
    }
    
    @RequestMapping("/deletedbatch")
    public String deleteProverInfoList() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
    	List<String>  ids = new ArrayList<>();
        ids.add("acb17958-7286-4332-b62c-a170bfc6dc7a");
        ids.add("8ba8278d-5239-48a3-9b9e-6584906edd82");
        proverInfoService.deleteProverInfoByUuidList(ids);
        return " nouce Hello World";
    }
    
    @RequestMapping("/verify")
    public String verify() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
        boolean ret = proverInfoService.verifyProverNonce("xia.tian@dxc.com", "896614");
        return "verification done";
    }
    
    
    @RequestMapping("/deleteProver")
    public String deleteProver() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
        proverInfoService.deleteProverInfo("xia.tian@dxc.com");
        return "verification done";
    }
    
    
    @RequestMapping("/proverinfo")
    public String getProver() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
    	ProverInfo temp = proverInfoService.findProverInfoByEmail("yulan111@dxc.com");
        return "verification done";
    }
    
    @RequestMapping("/deletetable")
    public String deletetable() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
    	proverInfoService.deleteAllProverInfo();
    	return "verification done";
    }
    
//    @RequestMapping("/test")
//    public String test() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
//    	proverInfoService.getExistingProverList("tt", "xx");
//    	return "verification done";
//    }
    
    @ExceptionHandler(value = SQLException.class)
    public ResponseEntity<Object> exception(SQLException exception) {
    	 return new ResponseEntity<>(exception.getMessage(), HttpStatus.NOT_FOUND);
    }
}
