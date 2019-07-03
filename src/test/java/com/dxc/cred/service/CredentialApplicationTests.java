package com.dxc.cred.service;


import com.dxc.cred.utils.PoolUtils;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults;
import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.did.DidJSONParameters;
import org.hyperledger.indy.sdk.did.DidResults;
import org.hyperledger.indy.sdk.did.DidResults.CreateAndStoreMyDidResult;
import org.hyperledger.indy.sdk.ledger.Ledger;
import org.hyperledger.indy.sdk.ledger.LedgerResults.ParseResponseResult;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.CompletableFuture;

import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.*;
import static org.hyperledger.indy.sdk.ledger.Ledger.buildNymRequest;
import static org.hyperledger.indy.sdk.ledger.Ledger.signAndSubmitRequest;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CredentialApplicationTests {

    @Autowired
    private JavaMailSender sender;
	
	@Before
	public void setUp() {

	}
	
	static void demoDXCCreateCredReq() throws Exception {
		System.out.println("Ledger sample -> started");
		String fablerSeed = "DXC_VPC0000000000000000000DXCVPC";
		
		
		String proveSeed = "DXC_VPC000000000000000000DXCTina";

		

		// Set protocol version 2 to work with Indy Node 1.4
		Pool.setProtocolVersion(PoolUtils.getProtocolVersion()).get();

		// 1. Create ledger config from genesis txn file
		String poolName = PoolUtils.createPoolLedgerConfig();
		Pool pool = Pool.openPoolLedger(poolName, "{}").get();

		// 2. Create and Open My Wallet
		String myWalletConfig = "{\"id\":\"myWallet\"}";
		String myWalletCredentials = "{\"key\":\"my_wallet_key\"}";
		Wallet.createWallet(myWalletConfig, myWalletCredentials).get();
		Wallet dxcWallet = Wallet.openWallet(myWalletConfig, myWalletCredentials).get();


		// 3. Create My Did
		DidJSONParameters.CreateAndStoreMyDidJSONParameter myDidJson =
				new DidJSONParameters.CreateAndStoreMyDidJSONParameter(null, fablerSeed, null, null);
		CreateAndStoreMyDidResult createMyDidResult = Did.createAndStoreMyDid(dxcWallet, myDidJson.toJson()).get();
		String myDid = createMyDidResult.getDid();
		String myVerkey = createMyDidResult.getVerkey();
		System.out.println("dxcDid:\n" + myDid);
		System.out.println("dxcVerkey:\n" + myVerkey);
		
		//4. get schema from chain
		String schemaId = "Th7MpTaRZVRYnPiabds81Y:2:gvt:8.0";
		String getSchemaRequest = Ledger.buildGetSchemaRequest(myDid, schemaId).get();
		System.out.println("getSchemaRequest:\n" + getSchemaRequest);
		String getSchemaResponse = Ledger.submitRequest(pool, getSchemaRequest).get();
		System.out.println("getSchemaResponse:\n" + getSchemaResponse);
		
        CompletableFuture<ParseResponseResult> schema = Ledger.parseGetSchemaResponse(getSchemaResponse);
        ParseResponseResult result = schema.get();
        String schemaDataJSON = result.getObjectJson();
        System.out.println("schemaDataJSON:\n" + schemaDataJSON);

		//4.3. Issuer create Credential Definition
		String credDefTag = "TAG3";
		String credDefConfigJson = "{\"support_revocation\":false}";
		AnoncredsResults.IssuerCreateAndStoreCredentialDefResult createCredDefResult =
				issuerCreateAndStoreCredentialDef(dxcWallet, myDid, schemaDataJSON, credDefTag, "CL", credDefConfigJson).get();
		String credDefId = createCredDefResult.getCredDefId();
		String credDefJson = createCredDefResult.getCredDefJson();
		System.out.println("credDefId:\n" + credDefId);
		System.out.println("credDefJson:\n" + credDefJson);

		String credDefRequest = Ledger.buildCredDefRequest(myDid, credDefJson).get();
		System.out.println("credDefRequest:\n" + credDefRequest);

		System.out.println("\n4. Sending the CRED request to the ledger\n");
		String credResponse = Ledger.signAndSubmitRequest(pool, dxcWallet, myDid, credDefRequest).get();
		System.out.println("credResponse:\n" + credResponse);

    	//7. Issuer Creates Credential OFFER
        String getCredDefRequest = Ledger.buildGetCredDefRequest(myDid, credDefId).get();
        String getCredDefResponse = Ledger.submitRequest(pool, getCredDefRequest).get();
        CompletableFuture<ParseResponseResult> cred = Ledger.parseGetCredDefResponse(getCredDefResponse);
        result = cred.get();
        System.out.println("credDefId: result.getId() \n" +  result.getId());
        String credDataJSON = result.getObjectJson();
        System.out.println("credDataJSON:\n" + credDataJSON);
		String credOffer = issuerCreateCredentialOffer(dxcWallet, result.getId()).get();
		 System.out.println("credOffer:\n" + credOffer);

		 
		 //8. Prover Create and Open Wallet
		String proverWalletConfig = "{\"id\":\"trusteeWallet\"}";
		String proverWalletCredentials = "{\"key\":\"prover_wallet_key\"}";
		Wallet.createWallet(proverWalletConfig, proverWalletCredentials).get();
		Wallet tinaWallet = Wallet.openWallet(proverWalletConfig, proverWalletCredentials).get();
		String masterSecretId = proverCreateMasterSecret(tinaWallet, null).get();
		
		// 3. Create Tina Did
		DidJSONParameters.CreateAndStoreMyDidJSONParameter tinaDidJson =
				new DidJSONParameters.CreateAndStoreMyDidJSONParameter(null, proveSeed, null, null);
		CreateAndStoreMyDidResult createTinaDidResult = Did.createAndStoreMyDid(dxcWallet, tinaDidJson.toJson()).get();
		String tinaDid = createTinaDidResult.getDid();
		String tinaVerkey = createTinaDidResult.getVerkey();
		System.out.println("tinaDid:\n" + tinaDid);
		System.out.println("tinaVerkey:\n" + tinaVerkey);
		
		//8. Prover Creates Credential Request
		AnoncredsResults.ProverCreateCredentialRequestResult createCredReqResult =
				proverCreateCredentialReq(tinaWallet, tinaDid, credOffer, credDataJSON, masterSecretId).get();
		String credReqJson = createCredReqResult.getCredentialRequestJson();
		String credReqMetadataJson = createCredReqResult.getCredentialRequestMetadataJson();
		
		//9. Issuer create Credential
		//   note that encoding is not standardized by Indy except that 32-bit integers are encoded as themselves. IS-786
		String credValuesJson = new JSONObject("{\n" +
				"        \"sex\": {\"raw\": \"male\", \"encoded\": \"594465709955896723921094925839488742869205008160769251991705001\"},\n" +
				"        \"name\": {\"raw\": \"Alex\", \"encoded\": \"1139481716457488690172217916278103335\"},\n" +
				"        \"height\": {\"raw\": \"175\", \"encoded\": \"175\"},\n" +
				"        \"age\": {\"raw\": \"28\", \"encoded\": \"28\"}\n" +
				"    }").toString();

		AnoncredsResults.IssuerCreateCredentialResult createCredentialResult =
				issuerCreateCredential(dxcWallet, credOffer, credReqJson, credValuesJson, null, - 1).get();
		String credential = createCredentialResult.getCredentialJson();
		System.out.println("credential:\n" + credential);
		
		// 8. Close and delete My Wallet
		dxcWallet.closeWallet().get();
		Wallet.deleteWallet(myWalletConfig, myWalletCredentials).get();

		//15. Close and Delete prover wallet
		tinaWallet.closeWallet().get();
		Wallet.deleteWallet(proverWalletConfig, proverWalletCredentials).get();

		// 10. Close Pool
		pool.closePoolLedger().get();

		// 11. Delete Pool ledger config
		Pool.deletePoolLedgerConfig(poolName).get();
//
		System.out.println("Ledger sample -> completed");
	}
	
	@Test
	public void demoDXCCreateCredOFFER() throws Exception {
		System.out.println("Ledger sample -> started");
		String fablerSeed = "DXC_VPC0000000000000000000DXCVPC";
		

		// Set protocol version 2 to work with Indy Node 1.4
		Pool.setProtocolVersion(PoolUtils.getProtocolVersion()).get();

		// 1. Create ledger config from genesis txn file
		String poolName = PoolUtils.createPoolLedgerConfig();
		Pool pool = Pool.openPoolLedger(poolName, "{}").get();

		// 2. Create and Open My Wallet
		String myWalletConfig = "{\"id\":\"myWallet\"}";
		String myWalletCredentials = "{\"key\":\"my_wallet_key\"}";
		Wallet.createWallet(myWalletConfig, myWalletCredentials).get();
		Wallet myWallet = Wallet.openWallet(myWalletConfig, myWalletCredentials).get();


		// 3. Create My Did
		DidJSONParameters.CreateAndStoreMyDidJSONParameter myDidJson =
				new DidJSONParameters.CreateAndStoreMyDidJSONParameter(null, fablerSeed, null, null);
		CreateAndStoreMyDidResult createMyDidResult = Did.createAndStoreMyDid(myWallet, myDidJson.toJson()).get();
		String myDid = createMyDidResult.getDid();
		String myVerkey = createMyDidResult.getVerkey();
		System.out.println("myDid:\n" + myDid);
		System.out.println("myVerkey:\n" + myVerkey);
		
		//4. get schema from chain
		String schemaId = "Th7MpTaRZVRYnPiabds81Y:2:gvt:8.0";
		String getSchemaRequest = Ledger.buildGetSchemaRequest(myDid, schemaId).get();
		System.out.println("getSchemaRequest:\n" + getSchemaRequest);
		String getSchemaResponse = Ledger.submitRequest(pool, getSchemaRequest).get();
		System.out.println("getSchemaResponse:\n" + getSchemaResponse);
		
        CompletableFuture<ParseResponseResult> schema = Ledger.parseGetSchemaResponse(getSchemaResponse);
        ParseResponseResult result = schema.get();
        String schemaDataJSON = result.getObjectJson();
        System.out.println("schemaDataJSON:\n" + schemaDataJSON);

		//4.3. Issuer create Credential Definition
		String credDefTag = "TAG2";
		String credDefConfigJson = "{\"support_revocation\":false}";
		AnoncredsResults.IssuerCreateAndStoreCredentialDefResult createCredDefResult =
				issuerCreateAndStoreCredentialDef(myWallet, myDid, schemaDataJSON, credDefTag, "CL", credDefConfigJson).get();
		String credDefId = createCredDefResult.getCredDefId();
		String credDefJson = createCredDefResult.getCredDefJson();
		System.out.println("credDefId:\n" + credDefId);
		System.out.println("credDefJson:\n" + credDefJson);

		String credDefRequest = Ledger.buildCredDefRequest(myDid, credDefJson).get();
		System.out.println("credDefRequest:\n" + credDefRequest);

		System.out.println("\n4. Sending the CRED request to the ledger\n");
		String credResponse = Ledger.signAndSubmitRequest(pool, myWallet, myDid, credDefRequest).get();
		System.out.println("credResponse:\n" + credResponse);

    	//7. Issuer Creates Credential Offer
        String getCredDefRequest = Ledger.buildGetCredDefRequest(myDid, credDefId).get();
        String getCredDefResponse = Ledger.submitRequest(pool, getCredDefRequest).get();
        CompletableFuture<ParseResponseResult> cred = Ledger.parseGetCredDefResponse(getCredDefResponse);
        result = cred.get();
        System.out.println("credDefId: result.getId() \n" +  result.getId());
        String credDataJSON = result.getObjectJson();
        System.out.println("credDataJSON:\n" + credDataJSON);
		String credOffer = issuerCreateCredentialOffer(myWallet, result.getId()).get();
		 System.out.println("credOffer:\n" + credOffer);

		// 8. Close and delete My Wallet
		myWallet.closeWallet().get();
		Wallet.deleteWallet(myWalletConfig, myWalletCredentials).get();


		// 10. Close Pool
		pool.closePoolLedger().get();

		// 11. Delete Pool ledger config
		Pool.deletePoolLedgerConfig(poolName).get();
//
		System.out.println("Ledger sample -> completed");
	}
	
	
	@Test
	public void demoTrusteeCreateDXC() throws Exception {
		System.out.println("Ledger sample -> started");

		String trusteeSeed = "000000000000000000000000Trustee1";
		String fablerSeed = "DXC_VPC0000000000000000000DXCVPC";

		// Set protocol version 2 to work with Indy Node 1.4
		Pool.setProtocolVersion(PoolUtils.getProtocolVersion()).get();

		// 1. Create ledger config from genesis txn file
		String poolName = PoolUtils.createPoolLedgerConfig();
		Pool pool = Pool.openPoolLedger(poolName, "{}").get();

		// 2. Create and Open My Wallet
		String myWalletConfig = "{\"id\":\"myWallet\"}";
		String myWalletCredentials = "{\"key\":\"my_wallet_key\"}";
		Wallet.createWallet(myWalletConfig, myWalletCredentials).get();
		Wallet myWallet = Wallet.openWallet(myWalletConfig, myWalletCredentials).get();

		// 3. Create and Open Trustee Wallet
		String trusteeWalletConfig = "{\"id\":\"theirWallet\"}";
		String trusteeWalletCredentials = "{\"key\":\"trustee_wallet_key\"}";
		Wallet.createWallet(trusteeWalletConfig, trusteeWalletCredentials).get();
		Wallet trusteeWallet = Wallet.openWallet(trusteeWalletConfig, trusteeWalletCredentials).get();

		// 4. Create My Did
		DidJSONParameters.CreateAndStoreMyDidJSONParameter myDidJson =
				new DidJSONParameters.CreateAndStoreMyDidJSONParameter(null, fablerSeed, null, null);
		CreateAndStoreMyDidResult createMyDidResult = Did.createAndStoreMyDid(myWallet, myDidJson.toJson()).get();
		String myDid = createMyDidResult.getDid();
		String myVerkey = createMyDidResult.getVerkey();
		System.out.println("myDid:\n" + myDid);
		System.out.println("myVerkey:\n" + myVerkey);

		// 5. Create Did from Trustee1 seed
		DidJSONParameters.CreateAndStoreMyDidJSONParameter theirDidJson =
				new DidJSONParameters.CreateAndStoreMyDidJSONParameter(null, trusteeSeed, null, null);

		CreateAndStoreMyDidResult createTheirDidResult = Did.createAndStoreMyDid(trusteeWallet, theirDidJson.toJson()).get();
		String trusteeDid = createTheirDidResult.getDid();
		System.out.println("trusteeDid:\n" + trusteeDid);
		System.out.println("trustee Verkey:\n" + createTheirDidResult.getVerkey());

		// 6. Build Nym Request
		//credResponse:
		//{"identifier":"JooXiHMwy34evdU6KNhGRF","reason":"client request invalid: UnauthorizedClientRequest('There is no accepted constraint',)","op":"REJECT","reqId":1553583739127102500}
		//String nymRequest = buildNymRequest(trusteeDid, myDid, myVerkey, null, null).get(); -> 	String nymRequest = buildNymRequest(trusteeDid, myDid, myVerkey, null, null).get();		
		String nymRequest = buildNymRequest(trusteeDid, myDid, myVerkey, null, "TRUST_ANCHOR").get();
		System.out.println("nymRequest:\n" + nymRequest);

		// 7. Trustee Sign Nym Request
		String nymResponseJson = signAndSubmitRequest(pool, trusteeWallet, trusteeDid, nymRequest).get();
		System.out.println("nymResponseJson:\n" + nymResponseJson);

		JSONObject nymResponse = new JSONObject(nymResponseJson);

		assertEquals(myDid, nymResponse.getJSONObject("result").getJSONObject("txn").getJSONObject("data").getString("dest"));
		assertEquals(myVerkey, nymResponse.getJSONObject("result").getJSONObject("txn").getJSONObject("data").getString("verkey"));

		// 8. Close and delete My Wallet
		myWallet.closeWallet().get();
	//	Wallet.deleteWallet(myWalletConfig, myWalletCredentials).get();

		// 9. Close and delete Their Wallet
		trusteeWallet.closeWallet().get();
//		Wallet.deleteWallet(trusteeWalletConfig, trusteeWalletCredentials).get();

		// 10. Close Pool
		pool.closePoolLedger().get();

		// 11. Delete Pool ledger config
		Pool.deletePoolLedgerConfig(poolName).get();
//
		System.out.println("Ledger sample -> completed");
	}
	
	
	@Test
	public void demoStewardCreateCred() throws Exception {
		
		
		// Set protocol version 2 to work with Indy Node 1.4
		Pool.setProtocolVersion(PoolUtils.getProtocolVersion()).get();
		
		//1. Create and Open Pool
		String DEFAULT_POOL_NAME = "default_pool";
		String poolName = PoolUtils.createPoolLedgerConfig();
		Pool pool = Pool.openPoolLedger(poolName, "{}").get();
		String stewardSeed = "000000000000000000000000Steward1";
		//String stewardSeed = "tx0000000000000000000000txeward2";
		
		//2. Issuer Create and Open Wallet		
		String issuerWalletConfig = "{\"id\":\"govWallet\"}";
		String issuerWalletCredentials = "{\"key\":\"gov_wallet_key\"}";
		Wallet.createWallet(issuerWalletConfig, issuerWalletCredentials).get();
		Wallet stewardWallet = Wallet.openWallet(issuerWalletConfig, issuerWalletCredentials).get();
		
		System.out.println("\n2. Generating and storing steward DID and Verkey\n");
		String did_json = "{\"seed\": \"" + stewardSeed + "\"}";
		DidResults.CreateAndStoreMyDidResult stewardResult = Did.createAndStoreMyDid(stewardWallet, did_json).get();
		//DidResults.CreateAndStoreMyDidResult stewardResult = Did.createAndStoreMyDid(issuerWallet, "{}").get();
		//CreateAndStoreMyDidResult createMyDidResult = Did.createAndStoreMyDid(myWallet, "{}").get();
		String stewardDid = stewardResult.getDid();
		System.out.println("Steward DID: " + stewardDid);
		System.out.println("Steward Verkey: " + stewardResult.getVerkey());
		String myVerkey = stewardResult.getVerkey();
		System.out.println("myDid:\n" + stewardDid);
		System.out.println("myVerkey:\n" + myVerkey);


		//3. Issuer create Schema Definition
		String schemaName = "gvt";
		String schemaVersion = "8.0";
		String schemaAttributes = "[\"name\", \"age\", \"sex\", \"height\"]";
		AnoncredsResults.IssuerCreateSchemaResult createSchemaResult =
				issuerCreateSchema(stewardDid, schemaName, schemaVersion, schemaAttributes).get();
		String schemaId = createSchemaResult.getSchemaId();
		String schemaDataJSON = createSchemaResult.getSchemaJson();
		System.out.println("schemaDataJSON:\n" + schemaDataJSON);

		String schemaRequest = Ledger.buildSchemaRequest(stewardDid, schemaDataJSON).get();
		System.out.println("Schema request:\n" + schemaRequest);
		
		System.out.println("\n3. Sending the SCHEMA request to the ledger\n");
		String schemaResponse = Ledger.signAndSubmitRequest(pool, stewardWallet, stewardDid, schemaRequest).get();
		System.out.println("Schema response:\n" + schemaResponse);
				
		String getSchemaRequest = Ledger.buildGetSchemaRequest(stewardDid, schemaId).get();
		System.out.println("getSchemaRequest response:\n" + schemaResponse);	

		
		
		//4. get schema from chain
	String schemaId1 = "Th7MpTaRZVRYnPiabds81Y:2:gvt:8.0";
	String getSchemaRequest1 = Ledger.buildGetSchemaRequest(stewardDid, schemaId1).get();
	System.out.println("getSchemaRequest:\n" + getSchemaRequest1);
	String getSchemaResponse = Ledger.submitRequest(pool, getSchemaRequest1).get();
	System.out.println("getSchemaResponse:\n" + getSchemaResponse);
	
    CompletableFuture<ParseResponseResult> cred = Ledger.parseGetSchemaResponse(getSchemaResponse);
    ParseResponseResult result = cred.get();
    String schemaDataJSON1 = result.getObjectJson();
    System.out.println("schemaDataJSON:\n" + schemaDataJSON1);
		
		//4.3. Issuer create Credential Definition
		String credDefTag = "TAG1";
		String credDefConfigJson = "{\"support_revocation\":false}";
		AnoncredsResults.IssuerCreateAndStoreCredentialDefResult createCredDefResult =
				issuerCreateAndStoreCredentialDef(stewardWallet, stewardDid, schemaDataJSON1, credDefTag, "CL", credDefConfigJson).get();
		String credDefId = createCredDefResult.getCredDefId();
		String credDefJson = createCredDefResult.getCredDefJson();
		System.out.println("credDefJson:\n" + credDefJson);

		String credDefRequest = Ledger.buildCredDefRequest(stewardDid, credDefJson).get();
		System.out.println("credDefRequest request:\n" + credDefRequest);

		System.out.println("\n4. Sending the CRED request to the ledger\n");
		//credResponse response:
		//{"identifier":"Th7MpTaRZVRYnPiabds81Y","reason":"client request invalid: InvalidClientRequest('validation error [ClientClaimDefSubmitOperation]: cannot be smaller than 1 (ref=0)',)","op":"REQNACK","reqId":1553584588109580700
		//get schema from chain can fix the issue		
		String credResponse = Ledger.signAndSubmitRequest(pool, stewardWallet, stewardDid, credDefRequest).get();
		System.out.println("credResponse response:\n" + credResponse);

		
		//14. Close and Delete issuer wallet
		stewardWallet.closeWallet().get();
		Wallet.deleteWallet(issuerWalletConfig, issuerWalletCredentials).get();

		//16. Close pool
		pool.closePoolLedger().get();
		Pool.deletePoolLedgerConfig(poolName).get();
	
	}
	
	@Test
	public void testIndyConnection() throws Exception {
		System.out.println("Anoncreds sample -> started");

		String issuerDid = "NcYxiDXkpYi6ov5FcYDi1e";
		String credId = "S5dwXz5w41mpwbcj9kuEZM:3:CL:58:TAG1";
		String credDef = "";

		// Set protocol version 2 to work with Indy Node 1.4
		Pool.setProtocolVersion(PoolUtils.getProtocolVersion()).get();

		//1. Create and Open Pool
		String poolName = PoolUtils.createPoolLedgerConfig();
		Pool pool = Pool.openPoolLedger(poolName, "{}").get();

		//2. Issuer Create and Open Wallet
		String issuerWalletConfig = "{\"id\":\"issuerWallet\"}";
		String issuerWalletCredentials = "{\"key\":\"issuer_wallet_key\"}";
		Wallet.createWallet(issuerWalletConfig, issuerWalletCredentials).get();
		Wallet issuerWallet = Wallet.openWallet(issuerWalletConfig, issuerWalletCredentials).get();

		credDef = getCredentialDefs(issuerDid, credId, pool);
		
		//7. Issuer Creates Credential Offer
		String credOffer = issuerCreateCredentialOffer(issuerWallet, credId).get();
		
		//14. Close and Delete issuer wallet
		issuerWallet.closeWallet().get();
		Wallet.deleteWallet(issuerWalletConfig, issuerWalletCredentials).get();		

		//16. Close pool
		pool.closePoolLedger().get();

		//17. Delete Pool ledger config
		Pool.deletePoolLedgerConfig(poolName).get();

		System.out.println("test indy successfully");    
	}
	
    private static String getCredentialDefs(String submitterDid, String credId, Pool pool){
        try {
            String getCredDefRequest = Ledger.buildGetCredDefRequest(submitterDid, credId).get();
            String getCredDefResponse = Ledger.submitRequest(pool, getCredDefRequest).get();
            CompletableFuture<ParseResponseResult> cred = Ledger.parseGetCredDefResponse(getCredDefResponse);
            ParseResponseResult result = cred.get();
    	//	String credDefId = result.getCredDefId();
            return result.getObjectJson();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
