package com.wimoor.amazon.adv.sd.service;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.github.miemiedev.mybatis.paginator.domain.PageBounds;
import com.github.miemiedev.mybatis.paginator.domain.PageList;
import com.wimoor.amazon.adv.common.service.IService;
import com.wimoor.amazon.adv.sd.pojo.AmzAdvProductTargeSD;
import com.wimoor.common.user.UserInfo;
 

public interface IAmzAdvProductTargeSDService  extends IService<AmzAdvProductTargeSD>{

	PageList<Map<String, Object>> getProductTargeList(Map<String, Object> map, PageBounds pageBounds);
	
	List<Map<String,Object>> getProductTargeChart(Map<String,Object> map);
	
	public Map<String,Object> getSumProductTarge(Map<String,Object> map);
	
	public List<AmzAdvProductTargeSD> amzCreateTargetingClauses_V3(UserInfo user,BigInteger  profileId, List<AmzAdvProductTargeSD> productTargeList) ;
	public AmzAdvProductTargeSD amzArchiveTargetingClause_V3(UserInfo user,BigInteger  profileId, String targetId) ;
	public List<AmzAdvProductTargeSD> amzUpdateTargetingClauses_V3(UserInfo user,BigInteger  profileId, List<AmzAdvProductTargeSD> productTargeList) ;
}
