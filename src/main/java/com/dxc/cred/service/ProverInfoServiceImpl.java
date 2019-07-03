package com.dxc.cred.service;

import com.dxc.cred.domain.ProverInfo;
import com.dxc.cred.domain.ProverInfoRowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class ProverInfoServiceImpl implements ProverInfoService {

	private static final String EMAIL_SUBJECT = "token information";
	private static final String EMAIL_CONTENT = "You are receiving this because your token has been generated successfully. \nThe token is %s.";

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	EmailServiceImpl emailService;

	private static final Logger logger = LoggerFactory.getLogger(ProverInfoServiceImpl.class);

	private static final String INSERT_PROVERINFO = "insert into prover_info(uuid, first_name, last_name, email, tenant, nonce, created_date, modified_date, expired_date) "
			+ "values(?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String QUERY_ACTIVE_INFO = "select * from prover_info where expired_date > ? and deleted is null order by modified_date desc";
	private static final String QUERY_EXPIRED_INFO = "select * from prover_info where expired_date <=  ? and deleted is null order by modified_date desc";
	private static final String QUERY_HISTORY_INFO = "select * from prover_info where deleted is not null order by modified_date desc";
    private static final String QUERY_PROVER_INFO_BY_EMAIL_AND_TENANT = "select * from prover_info where email = ? and tenant = ? and deleted is null";

	private static final String UPDATE_NOUNCE_BY_ID = "update prover_info set nonce = ?, modified_date = ?, expired_date = ?  where uuid = ?  and deleted is null";
	
	private static final String DELETED_FLAG_BY_ID = "update prover_info set deleted = ?, modified_date = ? where uuid = ?";
	
	private static final String DELETED_FLAG_BY_EMAIL = "update prover_info set deleted = ?, modified_date = ? where email = ? and deleted is null";
	
	private static final String QUERY_PROVER_INFO_BY_EMAIL_AND_NONCE = "select count(*) from prover_info where email = ? and nonce = ? and deleted is null and expired_date > ?";

    private static final String QUERY_PROVER_INFO_BY_EMAIL = "select * from prover_info where email = ? and deleted is null";


    private static final String DELETED_PROVER_INFO_TABLE = "delete from prover_info";
    
	private static final Random RANDOM = new SecureRandom();
	private static final String SYMBOLS = "0123456789";
	private static final String SRCEMAIL = "VPC_NOTIFICATION_NO_REPLY@DXC.COM";
	private static final long THREEDAYS_UNIT_MICROSEC= 1000*60*60*24*3;
	
    
	private List<ProverInfo> getExistingProverList(String email, String tenant) {
		 List<ProverInfo> ret = jdbcTemplate.query(QUERY_PROVER_INFO_BY_EMAIL_AND_TENANT, new Object[]{email, tenant},  new ProverInfoRowMapper());
		return ret;
	}
	
	@Override
	public List<ProverInfo> getActiveInfoList() {
    	String dt = getCurrentDateTime();    	
		return jdbcTemplate.query(QUERY_ACTIVE_INFO, new Object[] {dt}, new ProverInfoRowMapper());
	}
	
	@Override
	public List<ProverInfo> getExpiredInfoList() {
    	String dt = getCurrentDateTime();    	
		return jdbcTemplate.query(QUERY_EXPIRED_INFO, new Object[] {dt}, new ProverInfoRowMapper());
	}
	
	@Override
	public List<ProverInfo> getHistoryInfoList() {
		return jdbcTemplate.query(QUERY_HISTORY_INFO, new ProverInfoRowMapper());
	}
	
    @Override
    public void deleteAllProverInfo() {
		jdbcTemplate.update(DELETED_PROVER_INFO_TABLE);
    }
	
    
    @Override
    public ProverInfo findProverInfoByEmail(String email) {
    	return jdbcTemplate.queryForObject(QUERY_PROVER_INFO_BY_EMAIL,
                new Object[]{email}, 
                new ProverInfoRowMapper());
    }
    
    private ProverInfo findProverInfoByEmailAndTenant(String email, String tenant) {
    	ProverInfo ret = null; 
    	try{
        	ret = jdbcTemplate.queryForObject(QUERY_PROVER_INFO_BY_EMAIL_AND_TENANT,
                    new Object[]{email, tenant}, 
                    new ProverInfoRowMapper());
    	}
    	catch(EmptyResultDataAccessException e) {
    		logger.info("on history record");
    	}

    	return ret;
    }
	
    @Override
    public boolean verifyProverNonce(String email, String nonce){
    	String dt = getCurrentDateTime();    	
    	Integer count = jdbcTemplate.queryForObject(QUERY_PROVER_INFO_BY_EMAIL_AND_NONCE,
		new Object[]{email, nonce, dt}, 
		Integer.class);
    	if(0 == count) {
    		return false;
    	}	
    	return true;
    }
    
    @Override
    public boolean deleteProverInfo(String email) {
		String dt = getCurrentDateTime();
		int count = jdbcTemplate.update(DELETED_FLAG_BY_EMAIL, "1", dt, email);
		if(0 == count) {
			return false;
		}
		return true;
    }
    
	@Override
	public void createProverInfo(ProverInfo proverInfo) {
		//delete original data
		List<ProverInfo> existingList = getExistingProverList(proverInfo.getEmail(),proverInfo.getTenant());
		if(existingList!=null && existingList.size()>0) {
			deleteProverInfoByProverInfo(existingList);			
		}
		
		//create new data
		String currentDt = getCurrentDateTime();
		String expiredDt = getExpiredDateTime();
		String nonce = generateNonce();
		jdbcTemplate.update(INSERT_PROVERINFO, UUID.randomUUID().toString(), proverInfo.getFirstName(),
				proverInfo.getLastName(), proverInfo.getEmail(), proverInfo.getTenant(), nonce, currentDt, currentDt, expiredDt);
		logger.info("a record updated!");
	}

	@Override
	public void createProverInfoList(List<ProverInfo> proverInfoList) {
		//delete original data
		List<ProverInfo> existingList = new ArrayList<>();	
		proverInfoList.forEach(proverInfo -> {
			List<ProverInfo> list = getExistingProverList(proverInfo.getEmail(),proverInfo.getTenant());
			if(list!=null && list.size()>0) {
				existingList.addAll(list);		
			}
		});
		
		if(existingList!=null && existingList.size()>0) {
			deleteProverInfoByProverInfo(existingList);			
		}
		
		//create new data
		String currentDt = getCurrentDateTime();
		String expiredDt = getExpiredDateTime();
		jdbcTemplate.batchUpdate(INSERT_PROVERINFO, new BatchPreparedStatementSetter() {
			@Override
			public int getBatchSize() {
				return proverInfoList.size();
			}

			@Override
			public void setValues(java.sql.PreparedStatement ps, int i) throws SQLException {
				String nonce = generateNonce();
				ps.setString(1, UUID.randomUUID().toString());
				ps.setString(2, proverInfoList.get(i).getFirstName());
				ps.setString(3, proverInfoList.get(i).getLastName());
				ps.setString(4, proverInfoList.get(i).getEmail());
				ps.setString(5, proverInfoList.get(i).getTenant());
				ps.setString(6, nonce);
				ps.setString(7, currentDt);
				ps.setString(8, currentDt);
				ps.setString(9, expiredDt);
				proverInfoList.get(i).setNonce(nonce);
			}
		});
		
		proverInfoList.forEach(proverInfo -> {
			String content = String.format(EMAIL_CONTENT, proverInfo.getNonce());
			emailService.sendMail(SRCEMAIL, proverInfo.getEmail(), EMAIL_SUBJECT, content);
		});
		
		logger.info("batch records updated!");
	}

	public int updateNonceByUuid(ProverInfo prover) {		
		String nonce = generateNonce();
		String currentDt = getCurrentDateTime();
		String expiredDt = getExpiredDateTime();
		String content = String.format(EMAIL_CONTENT, nonce);
		int ret = jdbcTemplate.update(UPDATE_NOUNCE_BY_ID, nonce, currentDt, expiredDt, prover.getUuid());
		emailService.sendMail(SRCEMAIL, prover.getEmail(), EMAIL_SUBJECT, content);
		return ret;
	}
	
	public int[] updateNonceByUuidList(List<ProverInfo> proverList) {
		String currentDt = getCurrentDateTime();
		String expiredDt = getExpiredDateTime();
		int[] ret = jdbcTemplate.batchUpdate(UPDATE_NOUNCE_BY_ID, new BatchPreparedStatementSetter() {

			@Override
			public int getBatchSize() {
				return proverList.size();
			}

			@Override
			public void setValues(java.sql.PreparedStatement ps, int i) throws SQLException {
				String nonce = generateNonce();
				String content = String.format(EMAIL_CONTENT, nonce);
				ps.setString(1, nonce);
				ps.setString(2, currentDt);
				ps.setString(3, expiredDt);
				ps.setString(4, proverList.get(i).getUuid());
				emailService.sendMail(SRCEMAIL, proverList.get(i).getEmail(), EMAIL_SUBJECT, content);
			}
		});
		logger.info("batch records updated!");
		return ret;
	}
	
	
	public int deleteProverInfoByUuid(String id) {
		String dt = getCurrentDateTime();
		return jdbcTemplate.update(DELETED_FLAG_BY_ID, "1", dt, id);
	}
	
	public int deleteProverInfoByProverInfo(ProverInfo proverInfo) {
		String dt = getCurrentDateTime();
		return jdbcTemplate.update(DELETED_FLAG_BY_ID, "1", dt, proverInfo.getUuid());
	}
	
	public int[] deleteProverInfoByProverInfo(List<ProverInfo> proverInfoList) {
		String dt = getCurrentDateTime();

		int[] ret = jdbcTemplate.batchUpdate(DELETED_FLAG_BY_ID, new BatchPreparedStatementSetter() {

			@Override
			public int getBatchSize() {
				return proverInfoList.size();
			}

			@Override
			public void setValues(java.sql.PreparedStatement ps, int i) throws SQLException {
				ps.setString(1, "1");
				ps.setString(2, dt);
				ps.setString(3, proverInfoList.get(i).getUuid());
			}
		});
		logger.info("batch records updated!");
		return ret;
	}
	
	public int[] deleteProverInfoByUuidList(List<String> ids) {
		String dt = getCurrentDateTime();

		int[] ret = jdbcTemplate.batchUpdate(DELETED_FLAG_BY_ID, new BatchPreparedStatementSetter() {

			@Override
			public int getBatchSize() {
				return ids.size();
			}

			@Override
			public void setValues(java.sql.PreparedStatement ps, int i) throws SQLException {
				ps.setString(1, "1");
				ps.setString(2, dt);
				ps.setString(3, ids.get(i));
			}
		});
		logger.info("batch records updated!");
		return ret;
	}
	
	private String generateNonce() {

		char[] nonceChars = new char[6];

		for (int index = 0; index < nonceChars.length; ++index) {
			nonceChars[index] = SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length()));
		}
		return new String(nonceChars);
	}


	private String getCurrentDateTime() {
		Date current_date = new Date();
		SimpleDateFormat SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat.format(current_date.getTime());
		String dt = SimpleDateFormat.format(current_date.getTime());
		return dt;
	}
	
	private String getExpiredDateTime() {
		Date expired_data = new Date();
		long time = expired_data.getTime() + THREEDAYS_UNIT_MICROSEC;
		expired_data.setTime(time);
		SimpleDateFormat SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat.format(expired_data.getTime());
		String dt = SimpleDateFormat.format(expired_data.getTime());
		return dt;
	}

}