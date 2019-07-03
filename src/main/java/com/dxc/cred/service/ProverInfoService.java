package com.dxc.cred.service;

import java.util.List;

import com.dxc.cred.domain.ProverInfo;

public interface ProverInfoService {
	public void createProverInfo(ProverInfo proverInfo);
    public void createProverInfoList(List<ProverInfo> proverInfo);
    public int updateNonceByUuid(ProverInfo prover);
    public int[] updateNonceByUuidList(List<ProverInfo> proverList);
    public int deleteProverInfoByUuid(String id);
    public int[] deleteProverInfoByUuidList(List<String> ids);
    public boolean verifyProverNonce(String email, String nonce);
    public boolean deleteProverInfo(String email);
    public ProverInfo findProverInfoByEmail(String email);
    public void deleteAllProverInfo();
    public List<ProverInfo> getExpiredInfoList();
    public List<ProverInfo> getActiveInfoList();
    public List<ProverInfo> getHistoryInfoList();
}