package com.dxc.cred.domain;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ProverInfoRowMapper implements RowMapper<ProverInfo> {
	@Override
	public ProverInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
		ProverInfo proverInfo = new ProverInfo();
		proverInfo.setUuid(rs.getString("uuid"));
		proverInfo.setFirstName(rs.getString("first_name"));
		proverInfo.setLastName(rs.getString("last_name"));
		proverInfo.setEmail(rs.getString("email"));
		proverInfo.setTenant(rs.getString("tenant"));
		proverInfo.setCreatedDate(rs.getString("created_date"));
		proverInfo.setModifiedDate(rs.getString("modified_date"));
		proverInfo.setExpiredDate(rs.getString("expired_date"));
		return proverInfo;
	}
}
