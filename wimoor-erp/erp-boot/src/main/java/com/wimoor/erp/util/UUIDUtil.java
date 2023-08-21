package com.wimoor.erp.util;

import java.math.BigInteger;
import java.util.List;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import com.wimoor.util.SpringUtil;
 

public class UUIDUtil {
public static String getUUIDshort() {
	JdbcTemplate us=SpringUtil.getBean("jdbcTemplate");
	List<String> bigid = us.query("select uuid_short()",new BeanPropertyRowMapper<String>(String.class));
	if(bigid.size()>0) {
		return bigid.get(0);
	}
	return "0";
}
public static BigInteger getBigIntUUIDshort() {
	JdbcTemplate us=SpringUtil.getBean("jdbcTemplate");
	List<String> bigid = us.query("select uuid_short()",new BeanPropertyRowMapper<String>(String.class));
	if(bigid.size()>0) {
		return new BigInteger(bigid.get(0));
	}
	return new BigInteger("0");
}
}
