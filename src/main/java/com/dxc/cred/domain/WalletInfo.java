package com.dxc.cred.domain;

import java.util.ArrayList;
import java.util.List;

import org.hyperledger.indy.sdk.wallet.Wallet;

public class WalletInfo {
	private Wallet wallet;
	private String seed;
	private String walletConfig;
	private String walletKey;
	private List<DidKey> didKeyList;

	public WalletInfo(String seed, String walletConfig, String walletKey) {
		this.seed = seed;
		this.walletConfig = walletConfig;
		this.walletKey = walletKey;
		didKeyList = new ArrayList<>();
	}
	
	public WalletInfo() {
		didKeyList = new ArrayList<>();
	}
	
	public Wallet getWallet() {
		return wallet;
	}
	public void setWallet(Wallet wallet) {
		this.wallet = wallet;
	}
	public String getSeed() {
		return seed;
	}
	public void setSeed(String seed) {
		this.seed = seed;
	}
	public String getWalletConfig() {
		return walletConfig;
	}
	public void setWalletConfig(String walletConfig) {
		this.walletConfig = walletConfig;
	}
	public String getWalletKey() {
		return walletKey;
	}
	public void setWalletKey(String walletKey) {
		this.walletKey = walletKey;
	}
	public List<DidKey> getDidKeyList() {
		return didKeyList;
	}
	public void setDidKeyList(List<DidKey> didKeyList) {
		this.didKeyList = didKeyList;
	}
	public void print() {		
		System.out.println("wallet info start------------");
		System.out.println("wallet:" + wallet);
		System.out.println("seed:" + seed);
		System.out.println("walletConfig:" + walletConfig);
		System.out.println("walletKey:" + walletKey);
		for(DidKey elm: didKeyList) {
			elm.print();
		}
		System.out.println("wallet info end------------");
	}
}
