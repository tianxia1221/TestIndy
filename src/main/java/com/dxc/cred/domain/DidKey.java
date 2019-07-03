package com.dxc.cred.domain;

public class DidKey {
	private String did = null;
	private String verkey = null;

	public DidKey(String did, String verkey) {
		this.did = did;
		this.verkey = verkey;
	}
	public String getDid() {
		return did;
	}
	public void setDid(String did) {
		this.did = did;
	}
	public String getVerkey() {
		return verkey;
	}
	public void setVerkey(String verkey) {
		this.verkey = verkey;
	}
	public void print() {		
		System.out.println("did:" + did);
		System.out.println("verkey:" + verkey);
	}
}
