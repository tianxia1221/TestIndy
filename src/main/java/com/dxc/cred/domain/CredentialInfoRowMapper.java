package com.dxc.cred.domain;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class CredentialInfoRowMapper implements RowMapper<CredentialInfo> {
	@Override
	public CredentialInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
		CredentialInfo credentialInfo = new CredentialInfo();
		credentialInfo.setUuid(rs.getString("uuid"));
		credentialInfo.setEmail(rs.getString("email"));
		credentialInfo.setNonce(rs.getString("nonce"));
		credentialInfo.setOffer(rs.getString("offer"));
		credentialInfo.setCredential(rs.getString("credential"));
		credentialInfo.setCreatedDate(rs.getString("created_date"));
		credentialInfo.setModifiedDate(rs.getString("modified_date"));
//		credentialInfo.setUsername(rs.getString("username"));
		credentialInfo.setCredRegId(rs.getString("cred_reg_id"));
		credentialInfo.setDeleted(rs.getString("deleted"));
		return credentialInfo;
	}
}
