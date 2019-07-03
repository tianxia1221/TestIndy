package com.dxc.cred.service;

import com.dxc.cred.domain.CredDefIdInfo;
import com.dxc.cred.domain.CredentialInfo;
import com.dxc.cred.domain.DidKey;
import com.dxc.cred.domain.ProverInfo;
import com.dxc.cred.domain.WalletInfo;
import com.dxc.cred.err.BaseConfigurationException;
import com.dxc.cred.utils.EnvironmentUtils;
import com.dxc.cred.utils.PoolUtils;
import com.dxc.cred.utils.WalletUtils;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.anoncreds.Anoncreds;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults;
import org.hyperledger.indy.sdk.blob_storage.BlobStorageReader;
import org.hyperledger.indy.sdk.blob_storage.BlobStorageWriter;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.*;
import static org.hyperledger.indy.sdk.ledger.Ledger.buildNymRequest;
import static org.hyperledger.indy.sdk.ledger.Ledger.signAndSubmitRequest;

@Service
public class IndyDxcIssuerRevocServiceImpl implements IndyDxcIssuerRevocService{
	private static final String CRED_NAME_TRANSCRIPT = "transcript";
	private static final Logger logger = LoggerFactory.getLogger(IndyDxcIssuerRevocServiceImpl.class);
	private static final String CREDENTIAL_ID = "QfAEepouYkMtmCQFhL48sE:3:CL:12:TAG2";

	@Autowired
	private ProverInfoServiceImpl proverInfoService;
	
	@Autowired
	private CredentialInfoServiceImpl credentialInfoService;	
	
	@Autowired
	private CredDefIdInfoServiceImpl CredDefIdInfoService;	
	
	@Autowired
	private IndyProverServiceImpl IndyProverService;
	
	@Autowired
	private IndyVerifierServiceImpl indyVerifierService;

	private static Pool pool = null;
	
	private static WalletInfo dxcWalletInfo = null;
	private static Wallet dxcWallet = null;
	private static String dxcDid = null;
	private static String dxcVerkey = null;
	
	private static WalletInfo proverWalletInfo = null; 
	private static WalletInfo verifierWalletInfo = null; 
	
	// schema
	private static final String schemaName = "DXC";
	private static final String schemaVersion = "9.1";
	private static final String schemaAttributes = "[\"first_name\",\"last_name\", \"email\", \"tenant\"]";

	// credential
	private static final String credDefTag = "TAG1";
	private static final String credDefConfigJson = "{\"support_revocation\":true}";
	private static final String signatureType = "CL";
	private static final String REVOC_REG_TYPE = "CL_ACCUM";
	private static String credId = CREDENTIAL_ID;
	
	//revocation 
    String  revRegId = null; 
	String revRegDefConfig = null;
	String tailsWriterConfig = null;
	BlobStorageWriter tailsWriter = null;
	String revRegDefTag = "TAG1";
	String revRegDef = null;
	String revRegEntry = null;
	
	//For test
	String credRevId = null;
	BlobStorageReader blobStorageReaderCfg = null;
	int blobStorageReaderHandle = 0;

	// credential request jason value
	private static final String CREDENTIALVALUEJSON = "{\n"
			// + " \"sex\": {\"raw\": \"male\", \"encoded\":
			// \"594465709955896723921094925839488742869205008160769251991705001\"},\n"
			+ "        \"first_name\": {\"raw\": \"Xia\", \"encoded\": \"1139481716457488690172217916278103335\"},\n"
			+ "        \"last_name\": {\"raw\": \"Tian\", \"encoded\": \"1139481716457488690172217916278103336\"},\n"
			+ "        \"email\": {\"raw\": \"xia.tian@dxc.com\", \"encoded\": \"1139481716457488690172217916278103337\"},\n"
			+ "        \"tenant\": {\"raw\": \"VPC\", \"encoded\": \"1139481716457488690172217916278103338\"}\n"

			+ "    }";
	// + " \"department\": {\"raw\": \"VPC\", \"encoded\":
	// \"2139481716457488690172217916278103338\"}\n"+ " }";

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
	
	public void testdb() {
		CredDefIdInfo info = new CredDefIdInfo(UUID.randomUUID().toString(), CRED_NAME_TRANSCRIPT, "defid" , "revRegId");
		CredDefIdInfoService.createCredDefIdInfo(info);
		
		info = CredDefIdInfoService.findCredDefIdByCredName("transcript");
	}

	public void testCreateSchemaCred() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {
		
		// Issuer publish Prover DID
		onboardingProver();
		
		// Issuer publish Verifier DID
		onboardingVerifier();
		
		// Issuer posts Schema to Ledger
		String schemaId = buildDXCSchemaDef();
		
		// Issuer get Schema from Ledger
		String schemaDataJSON = getSchemaFromChain(pool, dxcDid, schemaId);
		credId = buildDXCRevocCredDef(schemaDataJSON);
		

		// Issuer creates Credential Offer
		String credOffer = getCredentialOffer(credId);	
		
		saveOfferInfo(credOffer, "", "xia.tian@dxc.com");
//		// save offer info
//		JSONObject offer = new JSONObject(credOffer);
//		String offerNonce = (String) offer.get("nonce");
//		CredentialInfo credentialInfo = new CredentialInfo(offerNonce, "xia.tian@dxc.com", "", credOffer);
//		credentialInfoService.createCredentialInfo(credentialInfo);
		
		// Prover creates Credential Request
		AnoncredsResults.ProverCreateCredentialRequestResult createCredReqResult = IndyProverService.proverCreateCredReq(credOffer);
		String credReqJson = createCredReqResult.getCredentialRequestJson();
		String credReqMetadataJson = createCredReqResult.getCredentialRequestMetadataJson();
		
		String cred = issueDXCCred(credOffer, credReqJson, CREDENTIALVALUEJSON).getCredentialJson();
     //   IndyProverService.exportRevocCred(credReqMetadataJson, cred, testCredDefJson, revRegDef);
        IndyProverService.storeRevocCredential(credReqMetadataJson, cred);
        
  ///////////////////////////////////////////////////////////////////////////////////
        String proofRequest = indyVerifierService.getProofRequest();
        String proof = IndyProverService.createProof(proofRequest, blobStorageReaderHandle, credRevId);        
        boolean ret = indyVerifierService.verify(proof);
        
        //revoke credential for credRegId user
        revokeCredential(revRegId, credRevId);
        Thread.sleep(3000);
        
        
        proofRequest = indyVerifierService.getProofRequest();
        proof = IndyProverService.createProof(proofRequest, blobStorageReaderHandle, credRevId); 
        ret = indyVerifierService.verify(proof);
        /////////////////////////////////////////////////////////////////////////////////
//        
//        proofRequest = indyVerifierService.getProofRequest();
//        proof = IndyProverService.createProofAfterRevoc(proofRequest, blobStorageReaderHandle, credRevId); 
//        ret = indyVerifierService.verify(proof);
	}
	
	public void verifyProof() throws JSONException, InterruptedException, ExecutionException, IndyException {
        String proofRequest = indyVerifierService.getProofRequest();
        String proof = IndyProverService.createProof(proofRequest, blobStorageReaderHandle, credRevId);        
        boolean ret = indyVerifierService.verify(proof);
	}
	
	
	public void revokeCredential(String revRegId, String credRevId) throws InterruptedException, ExecutionException, IndyException {
		// Issuer revokes credential
		String revRegDeltaJson = Anoncreds.issuerRevokeCredential(dxcWallet,
				blobStorageReaderHandle,
				revRegId, credRevId).get();

		// Issuer post RevocationRegistryDelta to Ledger
		String revRegEntryRequest = Ledger.buildRevocRegEntryRequest(dxcDid, revRegId, REVOC_REG_TYPE, revRegDeltaJson).get();

		String response = Ledger.signAndSubmitRequest(pool, dxcWallet, dxcDid, revRegEntryRequest).get();	
		System.out.println("revoke credential response:\n" + response);
	}
	
	public void onboardingProver() throws InterruptedException, ExecutionException, IndyException, JSONException {	
		DidKey proverDidKey = proverWalletInfo.getDidKeyList().get(0);
		String nymRequest = buildNymRequest(dxcDid, proverDidKey.getDid(), proverDidKey.getVerkey(), null, null).get();
		System.out.println("nymRequest:\n" + nymRequest);

		String nymResponseJson = signAndSubmitRequest(pool, dxcWallet, dxcDid, nymRequest).get();
		System.out.println("nymResponseJson:\n" + nymResponseJson);
	}
	
	public void onboardingVerifier() throws InterruptedException, ExecutionException, IndyException, JSONException {	
		DidKey verifierDidKey = verifierWalletInfo.getDidKeyList().get(0);
		String nymRequest = buildNymRequest(dxcDid, verifierDidKey.getDid(), verifierDidKey.getVerkey(), null, null).get();
		System.out.println("nymRequest:\n" + nymRequest);

		String nymResponseJson = signAndSubmitRequest(pool, dxcWallet, dxcDid, nymRequest).get();
		System.out.println("nymResponseJson:\n" + nymResponseJson);
	}

	public String buildDXCRevocCredDef(String schemaDataJSON)
			throws InterruptedException, ExecutionException, IndyException, JSONException {
		// Issuer creates CredentialDefinition
		AnoncredsResults.IssuerCreateAndStoreCredentialDefResult createCredDefResult = issuerCreateAndStoreCredentialDef(
				dxcWallet, dxcDid, schemaDataJSON, credDefTag, null, credDefConfigJson).get();
		String credDefId = createCredDefResult.getCredDefId();
		String credDefJson = createCredDefResult.getCredDefJson();
		System.out.println("credDefId:\n" + credDefId);
		System.out.println("credDefJson:\n" + credDefJson);

		// Issuer post CredentialDefinition to Ledger
		String credDefRequest = Ledger.buildCredDefRequest(dxcDid, credDefJson).get();
		System.out.println("credDefRequest:\n" + credDefRequest);

		System.out.println("\n. Sending the CRED request to the ledger\n");
		String credResponse = Ledger.signAndSubmitRequest(pool, dxcWallet, dxcDid, credDefRequest).get();
		System.out.println("credResponse:\n" + credResponse);
		
		testCredDefJson = credDefJson;
				
		//////////////////////////////
		//Issuer create Revocation Registry
		revRegDefConfig = new JSONObject("{\"issuance_type\":\"ISSUANCE_ON_DEMAND\",\"max_cred_num\":5}").toString();
		tailsWriterConfig = new JSONObject(String.format("{\"base_dir\":\"%s\", \"uri_pattern\":\"\"}", EnvironmentUtils.getIndyHomePath("tails")).replace('\\', '/')).toString();
		tailsWriter = BlobStorageWriter.openWriter("default", tailsWriterConfig).get();

//		revRegDefTag = "Tag2";
		AnoncredsResults.IssuerCreateAndStoreRevocRegResult createRevRegResult =
				issuerCreateAndStoreRevocReg(dxcWallet, dxcDid, null, 
						credDefTag, 
						credDefId, 
						revRegDefConfig, 
						tailsWriter).get();
		
		revRegId = createRevRegResult.getRevRegId();
		revRegDef = createRevRegResult.getRevRegDefJson();
		revRegEntry = createRevRegResult.getRevRegEntryJson();
		
		System.out.println("revRegDefId:\n" + revRegId);
		System.out.println("revRegDef:\n" + revRegDef);
		System.out.println("revRegEntry:\n" + revRegEntry);
		
		// Issuer posts RevocationRegistryDefinition to Ledger
		String revRegDefRequest = Ledger.buildRevocRegDefRequest(dxcDid, revRegDef).get();
		String res = Ledger.signAndSubmitRequest(pool, dxcWallet, dxcDid, revRegDefRequest).get();

		// Issuer posts RevocationRegistryEntry to Ledger
		String revRegEntryRequest = Ledger.buildRevocRegEntryRequest(dxcDid, revRegId, REVOC_REG_TYPE, revRegEntry).get();
		res = Ledger.signAndSubmitRequest(pool, dxcWallet, dxcDid, revRegEntryRequest).get();
		 
		//save credential id info to database
		CredDefIdInfo info = new CredDefIdInfo(UUID.randomUUID().toString(), CRED_NAME_TRANSCRIPT, credDefId, revRegId);
		CredDefIdInfoService.createCredDefIdInfo(info);
		return credDefId;
	}
	
	public AnoncredsResults.IssuerCreateCredentialResult issueDXCCred(String credOffer, String credReqJson, String credDataJSON)
			throws JSONException, InterruptedException, ExecutionException, IndyException {

		CredDefIdInfo info = CredDefIdInfoService.findCredDefIdByCredName(CRED_NAME_TRANSCRIPT);
		String revRegId = info.getRevRegId();
		
		// Issuer creates Credential
		blobStorageReaderCfg = BlobStorageReader.openReader("default", tailsWriterConfig).get();
		blobStorageReaderHandle = blobStorageReaderCfg.getBlobStorageReaderHandle();
		
		String credValuesJson = new JSONObject(credDataJSON).toString();
		AnoncredsResults.IssuerCreateCredentialResult createCredentialResult = issuerCreateCredential(dxcWallet,
				credOffer, credReqJson, credValuesJson, revRegId, blobStorageReaderHandle).get();
		
		String credential = createCredentialResult.getCredentialJson();
		credRevId = createCredentialResult.getRevocId();
		String revocRegDeltaJson = createCredentialResult.getRevocRegDeltaJson();
		System.out.println("credRevId:\n" + credRevId);
		System.out.println("revocRegDeltaJson:\n" + revocRegDeltaJson);
		System.out.println("revRegDefId:\n" + revRegId);
		System.out.println("credential:\n" + credential);
		
		// Issuer posts RevocationRegistryDelta to Ledger
		String revRegEntryRequest = Ledger.buildRevocRegEntryRequest(dxcDid, revRegId, REVOC_REG_TYPE, revocRegDeltaJson).get();
		String res = Ledger.signAndSubmitRequest(pool, dxcWallet, dxcDid, revRegEntryRequest).get();
		
		
		//save credential 
		JSONObject cred = new JSONObject(credential);
		String email = cred.getJSONObject("values").getJSONObject("email").getString("raw");
		CredentialInfo credentialInfo = credentialInfoService.findCredentialInfoByEmail(email);
		credentialInfo.setCredential(credential);
		credentialInfo.setCredRegId(credRevId);
		credentialInfoService.updateCredentialByUuid(credentialInfo);
		
		return createCredentialResult;
	}
	
	public String getCredentialOffer(String credDefId) throws InterruptedException, ExecutionException, IndyException {
		// //create cred offer
		String credOffer = issuerCreateCredentialOffer(dxcWallet, credDefId).get();
		System.out.println("credOffer:\n" + credOffer);

		return credOffer;
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
				return String.format(credOfferResFmt, "", "", 404, "Token is incorrect or the user is not existing!");
			}
			if (!credName.equals(CRED_NAME_TRANSCRIPT)) {
				logger.error("cred name is not transcript");
				return String.format(credOfferResFmt, "", "", 403, "credential is not existing!");
			}
			
			CredDefIdInfo credDefIdInfo = CredDefIdInfoService.findCredDefIdByCredName(CRED_NAME_TRANSCRIPT);
			String credDefId = credDefIdInfo.getCredDefId();	
			credDataJSON = getCredentialFromChain(pool, dxcDid, credDefId);
			credOffer = getCredentialOffer(credDefId);

			saveOfferInfo(credOffer, nonce, email);
		} catch (Exception e) {
			String msg = String.format(credOfferResFmt, "", "", 403, e.getMessage());
			logger.error(msg);
			return msg;
		}
		return String.format(credOfferResFmt, credOffer, credDataJSON, 200, "succeeded");
	}

	private void saveOfferInfo(String credOffer, String nonce, String email) throws JSONException {
		// save offer info
		JSONObject offer = new JSONObject(credOffer);
		String offerNonce = (String) offer.get("nonce");
		CredentialInfo credentialInfo = new CredentialInfo(offerNonce, email, nonce, credOffer);
		
		//only one credential, so delete others
		credentialInfoService.deleteByEmail(credentialInfo.getEmail());
		
		credentialInfoService.createCredentialInfo(credentialInfo);
	}

	public String createCredResponse(String credReq) {

		AnoncredsResults.IssuerCreateCredentialResult credResult = null;
		String cred = null;
		String credRevId = null;
		

		try {
			JSONObject credReqJson = new JSONObject(credReq);
			String credentialReq = (String) credReqJson.getJSONObject("value").get("credentialReq");
			String offerId = (String) credReqJson.getJSONObject("value").get("credentialOfferId");
			String email = (String) credReqJson.getJSONObject("value").get("email");

			CredentialInfo credentialInfo = credentialInfoService.findCredentialInfoById(offerId);
			if (!credentialInfo.getEmail().equals(email)) {
				logger.error("email info is incorrect");
				return String.format(credOfferResFmt, "", "", 403, "email info is incorrect!");
			}

			String offer = credentialInfo.getOffer();

			ProverInfo proverInfo = proverInfoService.findProverInfoByEmail(email);
			String credValue = String.format(credentialValueJsonFmt, proverInfo.getFirstName(),
					proverInfo.getLastName(), email, proverInfo.getTenant());

			credResult = issueDXCCred(offer, credentialReq, credValue);
			cred = credResult.getCredentialJson();
			credRevId = credResult.getRevocId();
			boolean ret = proverInfoService.deleteProverInfo(email);
			if (false == ret) {
				logger.error("cred name is not transcript");
				return String.format(credResFmt, "", 500, "internal server error!");
			}

			credentialInfo.setCredential(cred);
			credentialInfo.setCredRegId(credRevId);
			//save when generating credential
		//	credentialInfoService.updateCredentialByUuid(credentialInfo);
		} catch (Exception e) {
			String msg = String.format(credResFmt, "", 403, e.getMessage());
			logger.error(msg);
			return msg;
		}

		return String.format(credResFmt, cred, 200, "succeeded");
	}
	

	public String getCredentialFromChain(Pool pool, String submitterDid, String credDefId)
			throws InterruptedException, ExecutionException, IndyException {
		
		// Prover gets CredentialDefinition from Ledger
		String getCredDefRequest = Ledger.buildGetCredDefRequest(submitterDid, credDefId).get();
		String getCredDefResponse = Ledger.submitRequest(pool, getCredDefRequest).get();
		CompletableFuture<ParseResponseResult> cred = Ledger.parseGetCredDefResponse(getCredDefResponse);
		ParseResponseResult result = cred.get();
		System.out.println("credDefId: result.getId() \n" + result.getId());
		String credDataJSON = result.getObjectJson();
		System.out.println("credDataJSON:\n" + credDataJSON);
		return credDataJSON;
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

	
	public String buildDXCSchemaDef() throws InterruptedException, ExecutionException, IndyException {
		// Issuer creates Schema
		AnoncredsResults.IssuerCreateSchemaResult createSchemaResult = issuerCreateSchema(dxcDid, schemaName,
				schemaVersion, schemaAttributes).get();
		String schemaDataJSON = createSchemaResult.getSchemaJson();
		System.out.println("schemaDataJSON:\n" + schemaDataJSON);

		// Issuer posts Schema to Ledger
		String schemaRequest = Ledger.buildSchemaRequest(dxcDid, schemaDataJSON).get();
		System.out.println("Schema request:\n" + schemaRequest);

		System.out.println("\n3. Sending the SCHEMA request to the ledger\n");
		String schemaResponse = Ledger.signAndSubmitRequest(pool, dxcWallet, dxcDid, schemaRequest).get();
		System.out.println("Schema response:\n" + schemaResponse);
		return createSchemaResult.getSchemaId();
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
    	
		proverWalletInfo = WalletUtils.getProverWallet();
		if (null == proverWalletInfo) {
			return false;
		}	
		
		ret = indyVerifierService.init();
    	if(false == ret) {
    		throw new BaseConfigurationException("initilizaing IndyVerifierService failed");
    	}   	
		
    	verifierWalletInfo = WalletUtils.getVerifierWallet();
		if (null == verifierWalletInfo) {
			return false;
		}	
		
		return true;
	}
}

