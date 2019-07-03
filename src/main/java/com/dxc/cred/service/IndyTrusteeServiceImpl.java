package com.dxc.cred.service;

import static org.hyperledger.indy.sdk.ledger.Ledger.buildNymRequest;
import static org.hyperledger.indy.sdk.ledger.Ledger.signAndSubmitRequest;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.pool.Pool;
import org.json.JSONException;
import org.springframework.stereotype.Service;

import com.dxc.cred.domain.DidKey;
import com.dxc.cred.domain.WalletInfo;
import com.dxc.cred.utils.PoolUtils;
import com.dxc.cred.utils.WalletUtils;

@Service
public class IndyTrusteeServiceImpl implements IndyTrusteeService{
	
	private static Pool pool = null;
	private static WalletInfo trusteeWalletInfo = null;
	private static WalletInfo dxcWalletInfo = null;
	private static final String trustAnchor = "TRUST_ANCHOR";
	private static final String TRUSTEE = "TRUSTEE";
	
	public void onboardingDXC() throws InterruptedException, ExecutionException, IndyException, JSONException {
		DidKey trusteeDidKey = trusteeWalletInfo.getDidKeyList().get(0);
		DidKey dxcDidKey = dxcWalletInfo.getDidKeyList().get(0);	
		
		String nymRequest = buildNymRequest(trusteeDidKey.getDid(), dxcDidKey.getDid(), dxcDidKey.getVerkey(), null, TRUSTEE).get();
		System.out.println("nymRequest:\n" + nymRequest);

		String nymResponseJson = signAndSubmitRequest(pool, trusteeWalletInfo.getWallet(), trusteeDidKey.getDid(), nymRequest).get();
		System.out.println("nymResponseJson:\n" + nymResponseJson);
	}
	
	public boolean init() throws InterruptedException, ExecutionException, IndyException, IOException, JSONException {		
		pool = PoolUtils.getPool();
		if (null == pool) {
			return false;
		}
		
		trusteeWalletInfo = WalletUtils.getTrusteeWallet();
		if (null == trusteeWalletInfo) {
			return false;
		}
		
		dxcWalletInfo = WalletUtils.getDxcWallet();
		if (null == dxcWalletInfo) {
			return false;
		}		
		return true;
	}
}
