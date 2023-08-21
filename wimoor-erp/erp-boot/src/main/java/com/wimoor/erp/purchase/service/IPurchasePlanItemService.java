package com.wimoor.erp.purchase.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wimoor.erp.purchase.pojo.entity.PurchasePlanItem;

public interface IPurchasePlanItemService extends IService<PurchasePlanItem>{

	Map<String,Object> getSummary(String shopid, String planid);

	List<Map<String,Object>> getList(String shopid, String planid);

	List<Map<String,Object>> getList(String companyid, String planid, String batchnumber);

	void moveBatch(String shopid,String batchnumber);

	List<Map<String, Object>> getHisList(String shopid, String warehouseid);

}
