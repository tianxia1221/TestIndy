package com.dxc.cred.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.did.DidJSONParameters;
import org.hyperledger.indy.sdk.did.DidResults.CreateAndStoreMyDidResult;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.hyperledger.indy.sdk.wallet.WalletExistsException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.dxc.cred.domain.DidKey;
import com.dxc.cred.domain.WalletInfo;

public class WalletUtils {
	
	// DXC info
	private static WalletInfo dxcWalletInfo = null;
	private static final String dxcSeed = "DXC_VPC000000000000000000DXC_VPC";
	private static final String dxcWalletConfig = "{\"id\":\"dxcWallet\"}";
	private static final String dxcWalletCredentials = "{\"key\":\"dxc_wallet_key\"}";
	
	// Trustee info
	private static WalletInfo trusteeWalletInfo = null;
	private static final String trusteeSeed = "000000000000000000000000Trustee1";
//	private static final String trusteeSeed = "000000000000000000000000Steward1"; 
	private static final String trusteeWalletConfig = "{\"id\":\"trusteeWallet\"}";
	private static final String trusteeWalletCredentials = "{\"key\":\"trustee_wallet_key\"}";
	
	//Prover info
	private static WalletInfo proverWalletInfo = null;
	private static final String tinaSeed = "DXC_TINA140000000000XXXXXDXCTTXX";
	private static final String tinaWalletConfig = "{\"id\":\"tinaWallet\"}";
	private static final String tinaWalletCredentials = "{\"key\":\"tina_wallet_key\"}";
	
	//Prover info
	private static WalletInfo verifierWalletInfo = null;
	private static final String verifierSeed = "DXC_VPC000000000000XXXXXVerifier";
	private static final String verifierWalletConfig = "{\"id\":\"verifierWallet\"}";
	private static final String verifierWalletCredentials = "{\"key\":\"verifier_wallet_key\"}";
	
	public static WalletInfo getDxcWallet(){
		if(null != dxcWalletInfo && null != dxcWalletInfo.getWallet()) {
			return dxcWalletInfo;
		}	
		dxcWalletInfo = new WalletInfo(dxcSeed, dxcWalletConfig, dxcWalletCredentials);
		if(openWallet(dxcWalletInfo)) {
			return dxcWalletInfo;
		}
		return null;
	}
	
	public static WalletInfo getTrusteeWallet(){
		if(null != trusteeWalletInfo && null != trusteeWalletInfo.getWallet()) {
			return trusteeWalletInfo;
		}	
		trusteeWalletInfo = new WalletInfo(trusteeSeed, trusteeWalletConfig, trusteeWalletCredentials);
		if(openWallet(trusteeWalletInfo)) {
			return trusteeWalletInfo;
		}
		return null;
	}
	
	
	public static WalletInfo getProverWallet(){
		if(null != proverWalletInfo && null != proverWalletInfo.getWallet()) {
			return proverWalletInfo;
		}	
		proverWalletInfo = new WalletInfo(tinaSeed, tinaWalletConfig, tinaWalletCredentials);
		if(openWallet(proverWalletInfo)) {
			return proverWalletInfo;
		}
		return null;
	}
	
	public static WalletInfo getVerifierWallet(){
		if(null != verifierWalletInfo && null != verifierWalletInfo.getWallet()) {
			return verifierWalletInfo;
		}	
		verifierWalletInfo = new WalletInfo(verifierSeed, verifierWalletConfig, verifierWalletCredentials);
		if(openWallet(verifierWalletInfo)) {
			return verifierWalletInfo;
		}
		return null;
	}
	
	public static boolean openWallet(WalletInfo walletInfo){	
		boolean isWalletExisting = false;
		// create wallet
		try {
			Wallet.createWallet(walletInfo.getWalletConfig(), walletInfo.getWalletKey()).get();
		} catch (InterruptedException | ExecutionException | IndyException e) {
			if (e.getCause() instanceof WalletExistsException) {
				System.out.println(e.getCause());
				isWalletExisting = true;
			}
			else {
				e.printStackTrace();
				return false;
			}
		}
		
	
		if(!isWalletExisting) {
			// wallet is NOT existing
			try {
				Wallet wallet  = Wallet.openWallet(walletInfo.getWalletConfig(), walletInfo.getWalletKey()).get();
				walletInfo.setWallet(wallet);
				
				DidJSONParameters.CreateAndStoreMyDidJSONParameter trusteeDidJson = new DidJSONParameters.CreateAndStoreMyDidJSONParameter(
						null, walletInfo.getSeed(), null, null);
				CreateAndStoreMyDidResult createTheirDidResult = Did.createAndStoreMyDid(wallet, trusteeDidJson.toJson())
						.get();
				String did = createTheirDidResult.getDid();
				String verKey = createTheirDidResult.getVerkey();
				DidKey didKey = new DidKey(did, verKey);
				walletInfo.getDidKeyList().add(didKey);
				walletInfo.print();
				return true;
			} catch (InterruptedException | ExecutionException | IndyException e) {
					e.printStackTrace();
					return false;
			}
		}
		else {
			//wallet is existing
			CompletableFuture<String> comDid;
			String strDid;
			try {
				Wallet wallet  = Wallet.openWallet(walletInfo.getWalletConfig(), walletInfo.getWalletKey()).get();
				walletInfo.setWallet(wallet);
				
				comDid = Did.getListMyDidsWithMeta(wallet);
				strDid = comDid.get();
				JSONArray elms;
				elms = new JSONArray(strDid);
				for(int i = 0; i< elms.length(); i++) {
					JSONObject elm = elms.getJSONObject(i);			
					String did  = (String) elm.get("did");
					String verKey = (String) elm.get("verkey");
					DidKey didKey = new DidKey(did, verKey);
					walletInfo.getDidKeyList().add(didKey);
					walletInfo.print();
				}
				
			} catch (InterruptedException | ExecutionException | IndyException | JSONException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}
}
