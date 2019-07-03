package com.dxc.cred.domain;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CredDefIdInfoRowMapper implements RowMapper<CredDefIdInfo> {
    @Override
    public CredDefIdInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
    	CredDefIdInfo credDefIdInfo = new CredDefIdInfo();
        credDefIdInfo.setUuid(rs.getString("uuid"));
        credDefIdInfo.setCredName(rs.getString("cred_name"));
        credDefIdInfo.setCredDefId(rs.getString("cred_def_id"));
        credDefIdInfo.setRevRegId(rs.getString("rev_reg_id"));
        credDefIdInfo.setCreatedDate(rs.getString("created_date"));
        credDefIdInfo.setModifiedDate(rs.getString("modified_date"));
        credDefIdInfo.setDeleted(rs.getString("deleted"));
        return credDefIdInfo;
    }
}
