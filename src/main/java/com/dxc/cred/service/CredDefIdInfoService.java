package com.dxc.cred.service;

import com.dxc.cred.domain.CredDefIdInfo;

public interface CredDefIdInfoService {

	public CredDefIdInfo findCredDefIdByCredName(String cred_name);

	public void deleteAllCredDefIdInfo();

	public void createCredDefIdInfo(CredDefIdInfo info);
}
