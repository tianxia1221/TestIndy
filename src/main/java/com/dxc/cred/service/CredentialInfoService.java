package com.dxc.cred.service;

import com.dxc.cred.domain.CredentialInfo;

public interface CredentialInfoService {

	public void createCredentialInfo(CredentialInfo credentialInfo);

	public CredentialInfo findCredentialInfoById(String id);

	public int updateCredentialByUuid(CredentialInfo credential_info);

	public CredentialInfo findCredentialInfoByEmail(String email);

	public boolean deleteByEmail(String email);

}
