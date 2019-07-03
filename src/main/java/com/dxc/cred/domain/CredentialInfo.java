package com.dxc.cred.domain;

public class CredentialInfo {
	private String uuid;
	private String email;
	private String nonce;
	private String offer;
	private String credential;
	private String createdDate;
	private String modifiedDate;
//	private String username;
	private String credRegId;
	private String deleted;
	
	public CredentialInfo(String uuid, String email, String nonce, String offer) {
		this.uuid = uuid;
		this.email = email;
		this.nonce = nonce;
		this.offer = offer;
	}
	
	public CredentialInfo() {
	}

	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getNonce() {
		return nonce;
	}
	public void setNonce(String nonce) {
		this.nonce = nonce;
	}
	public String getOffer() {
		return offer;
	}
	public void setOffer(String offer) {
		this.offer = offer;
	}
	public String getCredential() {
		return credential;
	}
	public void setCredential(String credential) {
		this.credential = credential;
	}
	public String getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(String createdDate) {
		this.createdDate = createdDate;
	}
	public String getModifiedDate() {
		return modifiedDate;
	}
	public void setModifiedDate(String modifiedDate) {
		this.modifiedDate = modifiedDate;
	}
//	public String getUsername() {
//		return username;
//	}
//
//	public void setUsername(String username) {
//		this.username = username;
//	}

	public String getDeleted() {
		return deleted;
	}

	public void setDeleted(String deleted) {
		this.deleted = deleted;
	}

	public String getCredRegId() {
		return credRegId;
	}

	public void setCredRegId(String credRegId) {
		this.credRegId = credRegId;
	}

}
