package com.dxc.cred.service;

import java.util.concurrent.ExecutionException;

import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.anoncreds.Anoncreds;
import org.hyperledger.indy.sdk.ledger.Ledger;
import org.hyperledger.indy.sdk.ledger.LedgerResults;
import org.hyperledger.indy.sdk.ledger.LedgerResults.ParseResponseResult;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dxc.cred.domain.DidKey;
import com.dxc.cred.domain.WalletInfo;
import com.dxc.cred.utils.PoolUtils;
import com.dxc.cred.utils.WalletUtils;


@Service
public class IndyVerifierServiceImpl implements IndyVerifierService {
	private static final Logger logger = LoggerFactory.getLogger(IndyVerifierServiceImpl.class);
	
	private static Pool pool = null;
	
	private static WalletInfo verifierWalletInfo = null;
	private static Wallet verifierWallet = null;
	private static String verifierDid = null;
	private static String verifierVerkey = null;
	
	private static String proofRequest = null;
	
	//protected static final String verifierDid = "VsKV7grR1BUE29mG2Fm2kX";

	public boolean verify(String proofJson) throws JSONException, InterruptedException, ExecutionException, IndyException {		
		
		JSONObject proof = new JSONObject(proofJson);
		JSONObject identifier = proof.getJSONArray("identifiers").getJSONObject(0);
		
		// Verifier gets Schema from Ledger
		String getSchemaReq = Ledger.buildGetSchemaRequest(verifierDid, identifier.getString("schema_id")).get();
		String getSchemaResp = Ledger.submitRequest(pool, getSchemaReq).get();
		LedgerResults.ParseResponseResult schemaInfo3 = Ledger.parseGetSchemaResponse(getSchemaResp).get();
		String schemaId = schemaInfo3.getId();
		String schemaJson = schemaInfo3.getObjectJson();

		// Verifier gets CredDef from Ledger
		String getCredDefReq = Ledger.buildGetCredDefRequest(verifierDid, identifier.getString("cred_def_id")).get();
		String getCredDefResp = Ledger.submitRequest(pool, getCredDefReq).get();
		LedgerResults.ParseResponseResult credDefInfo3 = Ledger.parseGetCredDefResponse(getCredDefResp).get();
		String credDefId = credDefInfo3.getId();
		String credDefJson = credDefInfo3.getObjectJson();

		// Verifier gets RevocationRegistryDefinition from Ledger
		String getRevRegDefReq = Ledger.buildGetRevocRegDefRequest(verifierDid, identifier.getString("rev_reg_id")).get();
		String getRevRegDefResp = Ledger.submitRequest(pool, getRevRegDefReq).get();
		ParseResponseResult revRegDefInfo3 = Ledger.parseGetRevocRegDefResponse(getRevRegDefResp).get();
		String revRegDefId = revRegDefInfo3.getId();
		String revRegDefJson = revRegDefInfo3.getObjectJson();

		// Verifier gets RevocationRegistry from Ledger
		String getRevRegReq = Ledger.buildGetRevocRegRequest(verifierDid, identifier.getString("rev_reg_id"), identifier.getInt("timestamp")).get();
		String getRevRegResp = Ledger.submitRequest(pool, getRevRegReq).get();
		LedgerResults.ParseRegistryResponseResult revRegInfo3 = Ledger.parseGetRevocRegResponse(getRevRegResp).get();
		String revRegId = revRegInfo3.getId();
		String revRegJson = revRegInfo3.getObjectJson();
		long timestamp = revRegInfo3.getTimestamp();
		System.out.println("verify -- rev_reg_id --- timestamp: \n" + timestamp);

		String schemasJson = new JSONObject().put(schemaId, new JSONObject(schemaJson)).toString();
		String credDefsJson = new JSONObject().put(credDefId, new JSONObject(credDefJson)).toString();
		String revRegDefsJson = new JSONObject().put(revRegDefId, new JSONObject(revRegDefJson)).toString();
		String revRegsJson = new JSONObject().put(revRegId, new JSONObject().
				put(String.valueOf(timestamp), new JSONObject(revRegJson))).toString();

		Boolean valid = Anoncreds.verifierVerifyProof(proofRequest,
				proofJson,
				schemasJson,
				credDefsJson,
				revRegDefsJson,
				revRegsJson).get();
		System.out.println("verify -- valid: \n" + valid);
		return valid;
	}
	
	public boolean init() throws JSONException {		
		pool = PoolUtils.getPool();
		if (null == pool) {
			return false;
		}
		
		verifierWalletInfo = WalletUtils.getVerifierWallet();
		if (null == verifierWalletInfo) {
			return false;
		}
		verifierWallet = verifierWalletInfo.getWallet();
		DidKey didKey = verifierWalletInfo.getDidKeyList().get(0);	
		verifierDid = didKey.getDid();
		verifierVerkey = didKey.getVerkey();
		
		return true;
	}

	public static String getProofRequest() throws JSONException {
		long to = System.currentTimeMillis() / 1000;
		setProofRequest(new JSONObject().
				put("nonce", "123432421212").
				put("name", "NGP-LOGIN").
				put("version", "0.1").
				put("requested_attributes", new JSONObject().
						put("attr1_referent", new JSONObject().
								put("name", "first_name"))
					).
				put("requested_predicates", new JSONObject()).
				put("non_revoked", new JSONObject().
						put("to", to)).toString());
		return proofRequest;
	}

	public static void setProofRequest(String proofRequest) {
		IndyVerifierServiceImpl.proofRequest = proofRequest;
	}
}
