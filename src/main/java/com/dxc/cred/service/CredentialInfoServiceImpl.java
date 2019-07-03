package com.dxc.cred.service;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.dxc.cred.domain.CredentialInfo;
import com.dxc.cred.domain.CredentialInfoRowMapper;

@Service
public class CredentialInfoServiceImpl implements CredentialInfoService {

	@Autowired
    private JdbcTemplate jdbcTemplate;
	
	@Autowired
    private EmailServiceImpl emailService;

	private static final Logger logger = LoggerFactory.getLogger(CredentialInfoServiceImpl.class);

	private static final String INSERT_CREDENTIAL_INFO = "insert into credential_info(uuid, email, nonce, offer, created_date, modified_date) "
			+ "values(?, ?, ?, ?, ?, ?)";
	private static final String QUERY_CREDENTIAL_INFO_BY_ID = "select * from credential_info where uuid = ? and deleted is null";
	
	private static final String QUERY_CREDENTIAL_INFO_BY_EMAIL = "select * from credential_info where email = ? and deleted is null";

	private static final String UPDATE_CREDENTIAL_BY_ID = "update credential_info set credential = ?, cred_reg_id = ?, modified_date = ? where uuid = ?";
	
	private static final String DELETED_FLAG_BY_EMAIL = "update credential_info set deleted = ?, modified_date = ? where email = ? and deleted is null";

	@Override
	public void createCredentialInfo(CredentialInfo credentialInfo) {
		
		String dt = getCurrentDateTime();
		jdbcTemplate.update(INSERT_CREDENTIAL_INFO, credentialInfo.getUuid(), credentialInfo.getEmail(),
				credentialInfo.getNonce(), credentialInfo.getOffer(), dt, dt);
		logger.info("a record updated!");
	}

	@Override
	public CredentialInfo findCredentialInfoById(String id) {
		return jdbcTemplate.queryForObject(QUERY_CREDENTIAL_INFO_BY_ID, new Object[] { id },
				new CredentialInfoRowMapper());
	}
	
	@Override
	public CredentialInfo findCredentialInfoByEmail(String email) {
		return jdbcTemplate.queryForObject(QUERY_CREDENTIAL_INFO_BY_EMAIL, new Object[] { email },
				new CredentialInfoRowMapper());
	}

	@Override
	public int updateCredentialByUuid(CredentialInfo credential_info) {
		String currentDt = getCurrentDateTime();
		int ret = jdbcTemplate.update(UPDATE_CREDENTIAL_BY_ID, credential_info.getCredential(), credential_info.getCredRegId(), currentDt,
				credential_info.getUuid());
		return ret;
	}
	
    @Override
    public boolean deleteByEmail(String email) {
		String dt = getCurrentDateTime();
		int count = jdbcTemplate.update(DELETED_FLAG_BY_EMAIL, "1", dt, email);
		if(0 == count) {
			return false;
		}
		return true;
    }

	private String getCurrentDateTime() {
		Date current_date = new Date();
		SimpleDateFormat SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat.format(current_date.getTime());
		String dt = SimpleDateFormat.format(current_date.getTime());
		return dt;
	}

}
