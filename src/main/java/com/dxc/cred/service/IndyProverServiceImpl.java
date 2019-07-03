package com.dxc.cred.service;

import com.dxc.cred.domain.DidKey;
import com.dxc.cred.domain.WalletInfo;
import com.dxc.cred.utils.PoolUtils;
import com.dxc.cred.utils.WalletUtils;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.anoncreds.Anoncreds;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults;
import org.hyperledger.indy.sdk.anoncreds.CredentialsSearchForProofReq;
import org.hyperledger.indy.sdk.ledger.Ledger;
import org.hyperledger.indy.sdk.ledger.LedgerResults;
import org.hyperledger.indy.sdk.ledger.LedgerResults.ParseResponseResult;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.*;

@Service
public class IndyProverServiceImpl implements IndyProverService{
	private static final Logger logger = LoggerFactory.getLogger(IndyProverServiceImpl.class);
	private static Pool pool = null;
	private static WalletInfo proverWalletInfo = null;
	private static Wallet proverWallet = null;
	private static String proverDid = null;
	private static String proverVerkey = null;
	
	private static String testRevocRegDefJson = null;
	private static String  testMasterSecretId = null;
	private long to = 0;
	
	public String createProof(String proofRequest, int blobStorageReaderHandle, String credRevId ) throws JSONException, InterruptedException, ExecutionException, IndyException {
		System.out.println("createProof -- proofRequest --- : \n" + proofRequest);
		to = System.currentTimeMillis() / 1000;
		System.out.println("createProof -- to --- : \n" + to);
		// Prover gets Claims for Proof Request
		String credsJson = Anoncreds.proverGetCredentialsForProofReq(proverWallet, proofRequest).get();
		JSONObject credentials = new JSONObject(credsJson);
		JSONArray credsForReferent = credentials.getJSONObject("attrs").getJSONArray("attr1_referent");
		JSONObject cred_info = credsForReferent.getJSONObject(0).getJSONObject("cred_info");
		//String credentialIdForAttribute1 = cred_info.getString("referent");

		 
		// Prover gets RevocationRegistryDelta from Ledger
		String getRevRegDeltaRequest = Ledger.buildGetRevocRegDeltaRequest(proverDid, cred_info.getString("rev_reg_id"), - 1, (int) to).get();
		String getRevRegDeltaResponse = Ledger.submitRequest(pool, getRevRegDeltaRequest).get();
		LedgerResults.ParseRegistryResponseResult revRegInfo2 = Ledger.parseGetRevocRegDeltaResponse(getRevRegDeltaResponse).get();
		String revRegId = revRegInfo2.getId();
		String revocRegDeltaJson = revRegInfo2.getObjectJson();
		long timestamp = revRegInfo2.getTimestamp();
		
		
		// Prover gets RevocationRegistryDefinition
	//	String getRevRegDefRequest = Ledger.buildGetRevocRegDefRequest(proverDid, credential.getString("rev_reg_id")).get();
		String getRevRegDefRequest = Ledger.buildGetRevocRegDefRequest(proverDid, revRegId).get();
		String getRevRegDefResponse = Ledger.submitRequest(pool, getRevRegDefRequest).get();

		ParseResponseResult revRegInfo1 = Ledger.parseGetRevocRegDefResponse(getRevRegDefResponse).get();
		String revocRegDefJson = revRegInfo1.getObjectJson();
		
		// Prover creates RevocationState
		String revStateJson = Anoncreds.createRevocationState(/*blobStorageReaderHandle.getBlobStorageReaderHandle()*/blobStorageReaderHandle,
				revocRegDefJson, revocRegDeltaJson, timestamp, credRevId).get();
		
		
		// Prover gets Schema from Ledger
		String getSchemaRequest = Ledger.buildGetSchemaRequest(proverDid, cred_info.getString("schema_id")).get();
		String getSchemaResponse = Ledger.submitRequest(pool, getSchemaRequest).get();

		ParseResponseResult schemaInfo2 = Ledger.parseGetSchemaResponse(getSchemaResponse).get();
		String schemaId = schemaInfo2.getId();
		String schemaJson = schemaInfo2.getObjectJson();
		
		// Prover gets CredentialDefinition from Ledger
		String getCredDefRequest = Ledger.buildGetCredDefRequest(proverDid, cred_info.getString("cred_def_id")).get();

		String getCredDefResponse = Ledger.submitRequest(pool, getCredDefRequest).get();
		ParseResponseResult credDefIdInfo = Ledger.parseGetCredDefResponse(getCredDefResponse).get();

		String credDefId = credDefIdInfo.getId();
		String credDefJson = credDefIdInfo.getObjectJson();
		
		// Prover creates Proof

		String schemasJson = new JSONObject().put(schemaId, new JSONObject(schemaJson)).toString();
		String credDefsJson = new JSONObject().put(credDefId, new JSONObject(credDefJson)).toString();
		String revStatesJson = new JSONObject().put(revRegId, new JSONObject().
				put(String.valueOf(timestamp), new JSONObject(revStateJson))).toString();

		
        String requestedCredentialsJson = new JSONObject().
				put("self_attested_attributes", new JSONObject()).
				put("requested_attributes", new JSONObject().
						put("attr1_referent", new JSONObject().
								put("cred_id", cred_info.get("referent")).
								put("timestamp", timestamp).
								put("revealed", true))).
				put("requested_predicates", new JSONObject()).toString();
		
		String proofJson = Anoncreds.proverCreateProof(proverWallet, proofRequest,
				requestedCredentialsJson, testMasterSecretId,
				schemasJson, credDefsJson, revStatesJson).get();

		return proofJson;
	}
	
	
	public String createProofAfterRevoc(String proofRequest, int blobStorageReaderHandle, String credRevId ) throws JSONException, InterruptedException, ExecutionException, IndyException {
		
		long from = to;
		to = System.currentTimeMillis() / 1000;
		System.out.println("createProofAfterRevoc -- proofRequest --- : \n" + proofRequest);
		System.out.println("createProofAfterRevoc -- from --- : \n" + from);
		System.out.println("createProofAfterRevoc -- to --- : \n" + to);
		
		// Prover gets Claims for Proof Request
		String credsJson = Anoncreds.proverGetCredentialsForProofReq(proverWallet, proofRequest).get();
		JSONObject credentials = new JSONObject(credsJson);
		JSONArray credsForReferent = credentials.getJSONObject("attrs").getJSONArray("attr1_referent");
		JSONObject cred_info = credsForReferent.getJSONObject(0).getJSONObject("cred_info");
		//String credentialIdForAttribute1 = cred_info.getString("referent");

		 
		// Prover gets RevocationRegistryDelta from Ledger
		String getRevRegDeltaRequest = Ledger.buildGetRevocRegDeltaRequest(proverDid, cred_info.getString("rev_reg_id"), (int) from, (int) to).get();
		String getRevRegDeltaResponse = Ledger.submitRequest(pool, getRevRegDeltaRequest).get();
		LedgerResults.ParseRegistryResponseResult revRegInfo2 = Ledger.parseGetRevocRegDeltaResponse(getRevRegDeltaResponse).get();
		String revRegId = revRegInfo2.getId();
		String revocRegDeltaJson = revRegInfo2.getObjectJson();
		long timestamp = revRegInfo2.getTimestamp();
		
		
		// Prover gets RevocationRegistryDefinition
	//	String getRevRegDefRequest = Ledger.buildGetRevocRegDefRequest(proverDid, credential.getString("rev_reg_id")).get();
		String getRevRegDefRequest = Ledger.buildGetRevocRegDefRequest(proverDid, revRegId).get();
		String getRevRegDefResponse = Ledger.submitRequest(pool, getRevRegDefRequest).get();

		ParseResponseResult revRegInfo1 = Ledger.parseGetRevocRegDefResponse(getRevRegDefResponse).get();
		String revocRegDefJson = revRegInfo1.getObjectJson();
		
		// Prover creates RevocationState
		String revStateJson = Anoncreds.createRevocationState(/*blobStorageReaderHandle.getBlobStorageReaderHandle()*/blobStorageReaderHandle,
				revocRegDefJson, revocRegDeltaJson, timestamp, credRevId).get();
		
		
		// Prover gets Schema from Ledger
		String getSchemaRequest = Ledger.buildGetSchemaRequest(proverDid, cred_info.getString("schema_id")).get();
		String getSchemaResponse = Ledger.submitRequest(pool, getSchemaRequest).get();

		ParseResponseResult schemaInfo2 = Ledger.parseGetSchemaResponse(getSchemaResponse).get();
		String schemaId = schemaInfo2.getId();
		String schemaJson = schemaInfo2.getObjectJson();
		
		// Prover gets CredentialDefinition from Ledger
		String getCredDefRequest = Ledger.buildGetCredDefRequest(proverDid, cred_info.getString("cred_def_id")).get();

		String getCredDefResponse = Ledger.submitRequest(pool, getCredDefRequest).get();
		ParseResponseResult credDefIdInfo = Ledger.parseGetCredDefResponse(getCredDefResponse).get();

		String credDefId = credDefIdInfo.getId();
		String credDefJson = credDefIdInfo.getObjectJson();
		
		// Prover creates Proof

		String schemasJson = new JSONObject().put(schemaId, new JSONObject(schemaJson)).toString();
		String credDefsJson = new JSONObject().put(credDefId, new JSONObject(credDefJson)).toString();
		String revStatesJson = new JSONObject().put(revRegId, new JSONObject().
				put(String.valueOf(timestamp), new JSONObject(revStateJson))).toString();

		
        String requestedCredentialsJson = new JSONObject().
				put("self_attested_attributes", new JSONObject()).
				put("requested_attributes", new JSONObject().
						put("attr1_referent", new JSONObject().
								put("cred_id", cred_info.get("referent")).
								put("timestamp", timestamp).
								put("revealed", true))).
				put("requested_predicates", new JSONObject()).toString();
		
		String proofJson = Anoncreds.proverCreateProof(proverWallet, proofRequest,
				requestedCredentialsJson, testMasterSecretId,
				schemasJson, credDefsJson, revStatesJson).get();

		return proofJson;
	}
	
	
	public AnoncredsResults.ProverCreateCredentialRequestResult proverCreateCredReq(String credOffer)
			throws InterruptedException, ExecutionException, IndyException, JSONException {
		// Prover gets CredentialDefinition from Ledger
	    //String credDataJSON = getCredentialFromChain(pool, dxcDid, credId);		
		JSONObject credOfferJson = new JSONObject(credOffer);
		String credDataJSON = getCredentialFromChain(pool, proverDid, credOfferJson.getString("cred_def_id"));
		
		// Prover creates Credential Request
		DidKey proverDidKey = proverWalletInfo.getDidKeyList().get(0);
		// Prover creates Master Secret
		String masterSecretId = proverCreateMasterSecret(proverWalletInfo.getWallet(), null).get();
		testMasterSecretId = masterSecretId;
		AnoncredsResults.ProverCreateCredentialRequestResult createCredReqResult = proverCreateCredentialReq(proverWalletInfo.getWallet(),
				proverDidKey.getDid(), credOffer, credDataJSON, masterSecretId).get();
//		String credReqJson = createCredReqResult.getCredentialRequestJson();
//		String credReqMetadataJson = createCredReqResult.getCredentialRequestMetadataJson();
		return createCredReqResult;
	}

	public void exportCred(String credReqMetadataJson, String cred, String credDefJson) throws InterruptedException, ExecutionException, IndyException, JSONException {
		JSONObject walletKeyJson = new JSONObject(proverWalletInfo.getWalletKey());
		String EXPORT_KEY =  (String) walletKeyJson.get("key");		
		String EXPORT_CONFIG_JSON = "{ \"key\":\"" + EXPORT_KEY + "\", \"path\":\"" + "export_wallet" + "\"}";		
		
		// Prover Stores Credential
		proverStoreCredential(proverWalletInfo.getWallet(), null, credReqMetadataJson, cred, credDefJson, null).get();
		
		System.out.println("EXPORT_CONFIG_JSON:\n" + EXPORT_CONFIG_JSON);
		Wallet.exportWallet(proverWalletInfo.getWallet(), EXPORT_CONFIG_JSON).get();
	}

	
	public void storeCredential(String credReqMetadataJson, String cred, String credDefJson) throws InterruptedException, ExecutionException, IndyException, JSONException {		
		// Prover Stores Credential
		proverStoreCredential(proverWalletInfo.getWallet(), null, credReqMetadataJson, cred, credDefJson, null).get();
	}
	
	public void storeRevocCredential(String credReqMetadataJson, String cred) throws InterruptedException, ExecutionException, IndyException, JSONException {		

		// Prover gets CredentialDefinition from Ledger
		JSONObject credential = new JSONObject(cred);;
		String credDefJson = getCredentialFromChain(pool, proverDid, credential.getString("cred_def_id"));
		
		// Prover gets RevocationRegistryDefinition
		String getRevRegDefRequest = Ledger.buildGetRevocRegDefRequest(proverDid, credential.getString("rev_reg_id")).get();
		String getRevRegDefResponse = Ledger.submitRequest(pool, getRevRegDefRequest).get();

		ParseResponseResult revRegInfo1 = Ledger.parseGetRevocRegDefResponse(getRevRegDefResponse).get();
		String revocRegDefJson = revRegInfo1.getObjectJson();
		testRevocRegDefJson = revocRegDefJson;
		System.out.println("testRevocRegDefJson:\n" + testRevocRegDefJson);
		// Prover store received Credential
		proverStoreCredential(proverWalletInfo.getWallet(), "credential1_id_tina", credReqMetadataJson, cred, credDefJson, revocRegDefJson).get();
	}
	
	public void exportRevocCred(String credReqMetadataJson, String cred, String credDefJson, String revRegDefJson) throws InterruptedException, ExecutionException, IndyException, JSONException {		
		
		storeRevocCredential(credReqMetadataJson, cred);
		
		JSONObject walletKeyJson = new JSONObject(proverWalletInfo.getWalletKey());
		String EXPORT_KEY =  (String) walletKeyJson.get("key");		
		String EXPORT_CONFIG_JSON = "{ \"key\":\"" + EXPORT_KEY + "\", \"path\":\"" + "export_wallet" + "\"}";		
		System.out.println("EXPORT_CONFIG_JSON:\n" + EXPORT_CONFIG_JSON);
		Wallet.exportWallet(proverWalletInfo.getWallet(), EXPORT_CONFIG_JSON).get();
	}
	public boolean init() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {		
		pool = PoolUtils.getPool();
		if (null == pool) {
			return false;
		}
		
		proverWalletInfo = WalletUtils.getProverWallet();
		if (null == proverWalletInfo) {
			return false;
		}
		proverWallet = proverWalletInfo.getWallet();
		DidKey didKey = proverWalletInfo.getDidKeyList().get(0);	
		proverDid = didKey.getDid();
		proverVerkey = didKey.getVerkey();
		return true;
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
	
}
