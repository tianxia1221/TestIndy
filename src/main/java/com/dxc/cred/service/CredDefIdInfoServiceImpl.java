package com.dxc.cred.service;

import com.dxc.cred.domain.CredDefIdInfo;
import com.dxc.cred.domain.CredDefIdInfoRowMapper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CredDefIdInfoServiceImpl implements CredDefIdInfoService{

	@Autowired
    private JdbcTemplate jdbcTemplate;
	
	private static final Logger logger = LoggerFactory.getLogger(CredDefIdInfoServiceImpl.class);

	private static final String DELETED_RECORDS_TABLE = "delete from cred_def_id_info";
    private static final String INSERT_INFO = "insert into cred_def_id_info ( uuid, cred_name, cred_def_id, rev_reg_id, created_date, modified_date) "
            + "values(?, ?, ?, ?, ?, ?)";
    private static final String QUERY_CRED_INFO_BY_NAME = "select * from cred_def_id_info where cred_name = ? and deleted is null";

    @Override
    public CredDefIdInfo findCredDefIdByCredName(String cred_name) {
        logger.info("findProverInfoByEmail cred name" + cred_name);
        return jdbcTemplate.queryForObject(QUERY_CRED_INFO_BY_NAME,
                new Object[]{cred_name},
                new CredDefIdInfoRowMapper());
    }

    @Override
    public void deleteAllCredDefIdInfo() {
        logger.info("delete all cred def info!");
        jdbcTemplate.update(DELETED_RECORDS_TABLE);
    }

    @Override
    public void createCredDefIdInfo(CredDefIdInfo info) {
    	deleteAllCredDefIdInfo();
        //create new data
        String currentDt = getCurrentDateTime();
        jdbcTemplate.update(INSERT_INFO, UUID.randomUUID().toString(), info.getCredName(), info.getCredDefId(), info.getRevRegId(), currentDt, currentDt);
        logger.info("a credentialDefIdInfo record created!");
    }

    private String getCurrentDateTime() {
        Date current_date = new Date();
        SimpleDateFormat SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat.format(current_date.getTime());
        String dt = SimpleDateFormat.format(current_date.getTime());
        return dt;
    }
}
