package com.dxc.cred.service;

import com.dxc.cred.domain.CredentialInfo;
import com.dxc.cred.domain.DidKey;
import com.dxc.cred.domain.ProverInfo;
import com.dxc.cred.domain.WalletInfo;
import com.dxc.cred.err.BaseConfigurationException;
import com.dxc.cred.utils.PoolUtils;
import com.dxc.cred.utils.WalletUtils;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults;
import org.hyperledger.indy.sdk.ledger.Ledger;
import org.hyperledger.indy.sdk.ledger.LedgerResults.ParseResponseResult;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.*;

@Service
public class IndyDxcIssuerServiceImpl  implements IndyDxcIssuerService{
	
	private static final String EMPTY = "\"\"";
	private static final Logger logger = LoggerFactory.getLogger(IndyDxcIssuerServiceImpl.class);
	private static final String CREDENTIAL_ID = "8jrcCMa2hvEn2kKHxfFZUv:3:CL:119:TAG5";

	@Autowired
	private ProverInfoServiceImpl proverInfoService;
	
	@Autowired
	private CredentialInfoServiceImpl credentialInfoService;	
	

	@Autowired
	private IndyProverServiceImpl IndyProverService;
	
	@Autowired
	private IndyProverServiceImpl IndyVerifierService;

	private static Pool pool = null;
	
	private static WalletInfo dxcWalletInfo = null;
	private static Wallet dxcWallet = null;
	private static String dxcDid = null;
	private static String dxcVerkey = null;
	
	// schema
	private static final String schemaName = "DXC";
	private static final String schemaVersion = "5.1";
	private static final String schemaAttributes = "[\"first_name\",\"last_name\", \"email\", \"tenant\"]";

	// credential
	private static final String credDefTag = "TAG5";
	private static final String credDefConfigJson = "{\"support_revocation\":false}";
	private static final String signatureType = "CL";
	
	private static String credId = CREDENTIAL_ID;

	// credential request jason value
	private static final String credentialValueJson = "{\n"
			+ "        \"first_name\": {\"raw\": \"Xia\", \"encoded\": \"1139481716457488690172217916278103335\"},\n"
			+ "        \"last_name\": {\"raw\": \"Tian\", \"encoded\": \"1139481716457488690172217916278103336\"},\n"
			+ "        \"email\": {\"raw\": \"VPC@dxc.com\", \"encoded\": \"1139481716457488690172217916278103337\"},\n"
			+ "        \"tenant\": {\"raw\": \"VPC\", \"encoded\": \"1139481716457488690172217916278103338\"}\n"

			+ "    }";

	// credential request jason value format
	private static final String credentialValueJsonFmt = "{\n"
			+ "        \"first_name\": {\"raw\": \"%s\", \"encoded\": \"1139481716457488690172217916278103335\"},\n"
			+ "        \"last_name\": {\"raw\": \"%s\", \"encoded\": \"1139481716457488690172217916278103336\"},\n"
			+ "        \"email\": {\"raw\": \"%s\", \"encoded\": \"1139481716457488690172217916278103337\"},\n"
			+ "        \"tenant\": {\"raw\": \"%s\", \"encoded\": \"1139481716457488690172217916278103338\"}\n"
			+ "    }";

	private static final String credOfferResFmt = "{\n" + "  \"type\": \"credentialOfferResponse\",\n"
			+ "   \"value\": { \n" + " 	    \"credentialOffer\": %s,\n" + "         \"credentialDef\": %s\n"
			+ " 	  },\n" + " 	  \"src\": \"NGP_IDM\",\n" + " 	  \"status\":{\n" + " 	      \"code\":  %d,\n"
			+ " 	     \"message\": \"%s\"\n" + " 	   } \n" + " 		}";

	private static final String credResFmt = "{\n" + " 	\"type\": \"credentialResponse\",\n" + " 	  \"value\": {\n"
			+ " 	    \"credential\": %s\n" + " 	  },\n" + " 	  \"src\": \"NGP_IDM\",\n" + " 	  \"status\":{\n"
			+ " 	      \"code\":  %d,\n" + " 	      \"message\": \"%s\"\n" + " 	   } \n" + " 	}";

	
	private static String testCredDefJson = "";
	
	public static void main(String[] args) {
		final String EMPTY = "\"\"";
		System.out.println("EXPORT_CONFIG_JSON:\n" + EMPTY);
//		SpringApplication.run(DemoApplication.class, args);
	}
	public void testCreateSchemaCred() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
		String schemaId = buildDXCSchema();
		String schemaDataJSON = getSchemaFromChain(pool, dxcDid, schemaId);
		credId = buildDXCCredential(schemaDataJSON);
		String credDataJSON = getCredentialFromChain(pool, dxcDid, credId);
		String credOffer = getCredentialOffer(credId);
		
		AnoncredsResults.ProverCreateCredentialRequestResult createCredReqResult = IndyProverService.proverCreateCredReq(credOffer);
		String credReqJson = createCredReqResult.getCredentialRequestJson();
		String credReqMetadataJson = createCredReqResult.getCredentialRequestMetadataJson();
		
		String cred = dxcCreateCred(credOffer, credReqJson, credentialValueJson);

       IndyProverService.exportCred(credReqMetadataJson, cred, testCredDefJson);
	}
	
	public boolean init() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {		
		pool = PoolUtils.getPool();
		if (null == pool) {
			return false;
		}
		
		dxcWalletInfo = WalletUtils.getDxcWallet();
		if (null == dxcWalletInfo) {
			return false;
		}	
		
		DidKey dxcDidKey = dxcWalletInfo.getDidKeyList().get(0);	
		dxcDid = dxcDidKey.getDid();
		dxcVerkey = dxcDidKey.getVerkey();
		dxcWallet = dxcWalletInfo.getWallet();
		
		boolean ret = IndyProverService.init();
    	if(false == ret) {
    		throw new BaseConfigurationException("initilizaing IndyProverService failed");
    	}
		return true;
	}
	
	public String buildDXCSchema() throws InterruptedException, ExecutionException, IndyException {
		AnoncredsResults.IssuerCreateSchemaResult createSchemaResult = issuerCreateSchema(dxcDid, schemaName,
				schemaVersion, schemaAttributes).get();
		String schemaDataJSON = createSchemaResult.getSchemaJson();
		System.out.println("schemaDataJSON:\n" + schemaDataJSON);

		String schemaRequest = Ledger.buildSchemaRequest(dxcDid, schemaDataJSON).get();
		System.out.println("Schema request:\n" + schemaRequest);

		System.out.println("\n3. Sending the SCHEMA request to the ledger\n");
		String schemaResponse = Ledger.signAndSubmitRequest(pool, dxcWallet, dxcDid, schemaRequest).get();
		System.out.println("Schema response:\n" + schemaResponse);
		return createSchemaResult.getSchemaId();
	}

	public String getSchemaFromChain(Pool pool, String submitterDid, String schemaId)
			throws InterruptedException, ExecutionException, IndyException {
		String getSchemaRequest = Ledger.buildGetSchemaRequest(submitterDid, schemaId).get();
		System.out.println("getSchemaRequest:\n" + getSchemaRequest);
		String getSchemaResponse = Ledger.submitRequest(pool, getSchemaRequest).get();
		System.out.println("getSchemaResponse:\n" + getSchemaResponse);

		CompletableFuture<ParseResponseResult> cred = Ledger.parseGetSchemaResponse(getSchemaResponse);
		ParseResponseResult result = cred.get();
		String schemaDataJSON = result.getObjectJson();
		System.out.println("schemaDataJSON:\n" + schemaDataJSON);
		return schemaDataJSON;
	}

	public String buildDXCCredential(String schemaDataJSON)
			throws InterruptedException, ExecutionException, IndyException {
		AnoncredsResults.IssuerCreateAndStoreCredentialDefResult createCredDefResult = issuerCreateAndStoreCredentialDef(
				dxcWallet, dxcDid, schemaDataJSON, credDefTag, signatureType, credDefConfigJson).get();
		String credDefId = createCredDefResult.getCredDefId();
		String credDefJson = createCredDefResult.getCredDefJson();
		System.out.println("credDefId:\n" + credDefId);
		System.out.println("credDefJson:\n" + credDefJson);

		String credDefRequest = Ledger.buildCredDefRequest(dxcDid, credDefJson).get();
		System.out.println("credDefRequest:\n" + credDefRequest);

		System.out.println("\n4. Sending the CRED request to the ledger\n");
		String credResponse = Ledger.signAndSubmitRequest(pool, dxcWallet, dxcDid, credDefRequest).get();
		System.out.println("credResponse:\n" + credResponse);
		
		testCredDefJson = credDefJson;
		 
		return credDefId;
	}

	public String getCredentialFromChain(Pool pool, String submitterDid, String credDefId)
			throws InterruptedException, ExecutionException, IndyException {
		String getCredDefRequest = Ledger.buildGetCredDefRequest(submitterDid, credDefId).get();
		String getCredDefResponse = Ledger.submitRequest(pool, getCredDefRequest).get();
		CompletableFuture<ParseResponseResult> cred = Ledger.parseGetCredDefResponse(getCredDefResponse);
		ParseResponseResult result = cred.get();
		System.out.println("credDefId: result.getId() \n" + result.getId());
		String credDataJSON = result.getObjectJson();
		System.out.println("credDataJSON:\n" + credDataJSON);
		return credDataJSON;
	}
	
	public String createCredOfferResponse(String offerReq) {
		String credDataJSON = null;
		String credOffer = null;
		try {

			JSONObject offerReqJson = new JSONObject(offerReq);
			String credName = (String) offerReqJson.getJSONObject("value").get("credentialName");
			String nonce = (String) offerReqJson.getJSONObject("value").get("token");
			String email = (String) offerReqJson.getJSONObject("value").get("email");

			if (!proverInfoService.verifyProverNonce(email, nonce)) {
				logger.error("email:" + email);
				logger.error("nonce:" + nonce);
				return String.format(credOfferResFmt, EMPTY, EMPTY, 404, "Token is incorrect/expired or the user is not existing!");
			}
			if (!credName.equals("transcript")) {
				logger.error("cred name is not transcript");
				return String.format(credOfferResFmt, EMPTY, EMPTY, 403, "credential is not existing!");
			}
			credDataJSON = getCredentialFromChain(pool, dxcDid, CREDENTIAL_ID);
			credOffer = getCredentialOffer(CREDENTIAL_ID);

			// save offer info
			JSONObject offer = new JSONObject(credOffer);
			String offerNonce = (String) offer.get("nonce");
			
			CredentialInfo credentialInfo = new CredentialInfo(offerNonce, email, nonce, credOffer);
			credentialInfoService.createCredentialInfo(credentialInfo);
		} catch (Exception e) {
			String msg = String.format(credOfferResFmt, EMPTY, EMPTY, 403, e.getMessage());
			logger.error(msg);
			return msg;
		}
		return String.format(credOfferResFmt, credOffer, credDataJSON, 200, "succeeded");
	}

	public String createCredResponse(String credReq) {

		String cred = null;

		try {
			JSONObject credReqJson = new JSONObject(credReq);
			String credentialReq = (String) credReqJson.getJSONObject("value").get("credentialReq");
			String offerId = (String) credReqJson.getJSONObject("value").get("credentialOfferId");
			String email = (String) credReqJson.getJSONObject("value").get("email");

			CredentialInfo credentialInfo = credentialInfoService.findCredentialInfoById(offerId);
			if (!credentialInfo.getEmail().equals(email)) {
				logger.error("email info is incorrect");
				return String.format(credResFmt, EMPTY, 403, "email info is incorrect!");
			}

			String offer = credentialInfo.getOffer();

			ProverInfo proverInfo = proverInfoService.findProverInfoByEmail(email);
			String credValue = String.format(credentialValueJsonFmt, proverInfo.getFirstName(),
					proverInfo.getLastName(), email, proverInfo.getTenant());

			cred = dxcCreateCred(offer, credentialReq, credValue);

			boolean ret = proverInfoService.deleteProverInfo(email);
			if (false == ret) {
				logger.error("cred name is not transcript");
				return String.format(credResFmt, EMPTY, 500, "internal server error!");
			}

			credentialInfo.setCredential(cred);
			credentialInfoService.updateCredentialByUuid(credentialInfo);
		} catch (Exception e) {
			String msg = String.format(credResFmt, EMPTY, 403, e.getMessage());
			logger.error(msg);
			return msg;
		}

		return String.format(credResFmt, cred, 200, "succeeded");
	}
	
	public String dxcCreateCred(String credOffer, String credReqJson, String credDataJSON)
			throws JSONException, InterruptedException, ExecutionException, IndyException {
		//for test
		//String credValuesJson = new JSONObject(credentialValueJson).toString();
		String credValuesJson = new JSONObject(credDataJSON).toString();
		AnoncredsResults.IssuerCreateCredentialResult createCredentialResult = issuerCreateCredential(dxcWallet,
				credOffer, credReqJson, credValuesJson, null, -1).get();
		String credential = createCredentialResult.getCredentialJson();
		System.out.println("credential:\n" + credential);
		return credential;
	}
	
	public String getCredentialOffer(String credDefId) throws InterruptedException, ExecutionException, IndyException {
		// //create cred offer
		String credOffer = issuerCreateCredentialOffer(dxcWallet, credDefId).get();
		System.out.println("credOffer:\n" + credOffer);

		return credOffer;
	}
	

}
