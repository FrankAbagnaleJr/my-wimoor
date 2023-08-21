package com.wimoor.amazon.finances.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wimoor.amazon.finances.pojo.entity.AmzSettlementAccStatement;
import com.wimoor.common.user.UserInfo;

public interface IAmzSettlementAccStatementService  extends IService<AmzSettlementAccStatement>{
 	public Map<String, Object> saveFinStatement(UserInfo user,Map<String, Object> map, List<Map<String, Object>> list, Map<String, String> field);
	public Integer deleteAmzSettlementAccStatement(String id);
	public List<Map<String, Object>> findAmzSettlementAccStatement(String shopid) ;
	public AmzSettlementAccStatement findCommodityStatement(String id);
}
