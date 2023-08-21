package com.wimoor.amazon.product.controller;


import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.amazon.spapi.model.productpricing.GetPricingResponse;
import com.wimoor.amazon.api.ErpClientOneFeignManager;
import com.wimoor.amazon.auth.pojo.entity.AmazonAuthority;
import com.wimoor.amazon.auth.service.IAmazonAuthorityService;
import com.wimoor.amazon.common.pojo.entity.DaysalesFormula;
import com.wimoor.amazon.common.service.IDaysalesFormulaService;
import com.wimoor.amazon.product.pojo.entity.AmzProductPriceRecord;
import com.wimoor.amazon.product.pojo.entity.ProductInOpt;
import com.wimoor.amazon.product.pojo.entity.ProductInfo;
import com.wimoor.amazon.product.pojo.entity.ProductPrice;
import com.wimoor.amazon.product.service.IProductProductPriceService;
import com.wimoor.amazon.product.service.IAmzProductPriceRecordService;
import com.wimoor.amazon.product.service.IProductInOptService;
import com.wimoor.amazon.product.service.IProductInfoService;
import com.wimoor.common.result.Result;
import com.wimoor.common.service.impl.SystemControllerLog;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * 产品信息 前端控制器
 * </p>
 *
 * @author wimoor team
 * @since 2022-05-27
 */
@RestController
@RequiredArgsConstructor
@Component("productInOptController")
@SystemControllerLog("产品扩展信息")
@RequestMapping("/api/v1/report/product/productInOpt")
public class ProductInOptController {
	final IProductInOptService iProductInOptService;
	final IProductInfoService iProductInfoService;
	final IAmazonAuthorityService amazonAuthorityService;
	final IProductProductPriceService iProductCaptureProductPriceService;
	final IDaysalesFormulaService daysalesFormulaService;
	final IAmzProductPriceRecordService amzProductPriceRecordService;
	final ErpClientOneFeignManager erpClientOneFeignManager;
    @GetMapping("/refreshAllProductAdv")
    public Result<String> refreshAllProductAdvAction() {
    	new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				iProductInOptService.refreshAllProductAdv();
			}
    	}).start();
        return Result.success("true");
    }
    
    @GetMapping("/updateOptMsku")
    public Result<String> updateOptMskuAction(String pid,String msku) {
    	UserInfo user = UserInfoContext.get();
    	ProductInOpt opt = iProductInOptService.getById(pid);
    	ProductInfo info = iProductInfoService.getById(pid);
    	AmazonAuthority auth = amazonAuthorityService.getById(info.getAmazonAuthId());
    	String ownerid=null;
    	try {
    		Result<Map<String, Object>> result = erpClientOneFeignManager.getMaterialBySKUAction(msku, auth.getShopId());
    		if(Result.isSuccess(result)&&result.getData()!=null) {
    			Map<String, Object> materialMap = result.getData();
    			if(materialMap.get("ownerid")!=null) {
    				ownerid=materialMap.get("ownerid").toString();
    			}
    		}
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    	if(opt!=null) {
    		if(StrUtil.isNotEmpty(msku)) {
    			opt.setMsku(msku);
    			opt.setOperator(new BigInteger(user.getId()));
    			opt.setLastupdate(new Date());
    			if(opt.getOwner()==null) {
    				opt.setOwner(ownerid);
    			}
				iProductInOptService.updateById(opt);
				return Result.success("isok");
    		}
    	}else {
    		//做新增
    		if(StrUtil.isNotEmpty(msku)) {
    			opt=new ProductInOpt();
    			opt.setPid(new BigInteger(pid));
    			opt.setMsku(msku);
    			opt.setDisable(false);
    			opt.setOperator(new BigInteger(user.getId()));
    			opt.setLastupdate(new Date());
    			opt.setOwner(ownerid);
				iProductInOptService.save(opt);
				return Result.success("isok");
    		}
    	}
    	
    	return Result.success("fail");
        
    }
    
    @GetMapping("/updateOptProfitId")
	public Result<Map<String,Object>> updateOptProfitIdAction(String profitid,String pid) {
		Map<String,Object> maps=new HashMap<String, Object>();
		ProductInOpt productOpt = iProductInOptService.getById(pid);
		Boolean result=false;
		if(productOpt!=null) {
			if(StrUtil.isNotEmpty(profitid)) {
				productOpt.setProfitid(new BigInteger(profitid));
			}else {
				productOpt.setProfitid(null);
			}
			productOpt.setLastupdate(new Date());
			result=iProductInOptService.updateById(productOpt);
		}
		if(result) {
			maps.put("isok", "true");
		}else {
			maps.put("isok", "false");
		}
		return Result.success(maps);
	}
    
    @GetMapping("/getProRemarkHis")
	public Result<List<Map<String,Object>>> getProRemarkHisAction(String  pid,String ftype) {
		return Result.success(iProductInOptService.getProRemarkHis(pid, ftype));
	}
    
    @GetMapping("/getOptStatusById")
   	public Result<Integer> getOptStatusByIdAction(String  pid) {
    	ProductInOpt opt = iProductInOptService.getById(pid);
    	if(opt!=null) {
    		return Result.success(opt.getStatus());
    	}else {
    		return Result.success(null);
    	}
   	}
    
    @GetMapping("/updateOptStatus")
   	public Result<String> getOptStatusByIdAction(String pid,String status) {
    	UserInfo user = UserInfoContext.get();
    	ProductInOpt opt = iProductInOptService.getById(pid);
    	if(opt!=null) {
    		if("delete".equals(status)) {
    			opt.setStatus(null);
    		}else {
    			opt.setStatus(Integer.parseInt(status));
    		}
    		opt.setLastupdate(new Date());
    		opt.setOperator(new BigInteger(user.getId()));
    		iProductInOptService.updateById(opt);
    		return Result.success("ok");
    	}else {
    		opt=new ProductInOpt();
    		opt.setPid(new BigInteger(pid));
    		if("delete".equals(status)) {
    			opt.setStatus(null);
    		}else {
    			opt.setStatus(Integer.parseInt(status));
    		}
    		opt.setLastupdate(new Date());
    		opt.setOperator(new BigInteger(user.getId()));
    		iProductInOptService.save(opt);
    		return Result.success("ok");
    	}
   	}
    
    @GetMapping("/findPriceById")
   	public  Result<List<ProductPrice>> findPriceByIdAction(String  pid) {
   		return Result.success(iProductInOptService.findPrice(pid));
   	}
    
    @GetMapping("/saveProductTags")
    public Result<List<Map<String,Object>>> saveProductTagsAction(String pid,String ids) {
    	UserInfo user = UserInfoContext.get();
    	List<Map<String, Object>> result = iProductInOptService.saveTagsByPid(pid, ids, user.getId());
    	return Result.success(result);
    }
    
    @GetMapping("/findProductTags")
    public Result<String> findProductTagsAction(String pid) {
    	String strs=iProductInOptService.findProductTagsByPid(pid);
    	return Result.success(strs);
    }
    
    @GetMapping("/findOwnerById")
   	public  Result<String> findOwnerByIdAction(String  pid) {
   		return Result.success(iProductInOptService.findOwnerById(pid));
   	}
    
    @GetMapping("/updateOwnerById")
    @SystemControllerLog("更新运营负责人")
   	public  Result<String> updateOwnerByIdAction(String  pid,String ownerid) {
    	UserInfo user = UserInfoContext.get();
   		return Result.success(iProductInOptService.updateOwnerById(user,pid,ownerid));
   	}
    
	@GetMapping("/refreshPrice")
	public Result<?> refreshPriceAction(String pid) {
		ProductInfo product = iProductInfoService.getById(pid);
		AmazonAuthority auth = amazonAuthorityService.getById(product.getAmazonAuthId());
		GetPricingResponse response = iProductCaptureProductPriceService.captureProductPrice(auth, product.getSku(),product.getMarketplaceid());
		iProductCaptureProductPriceService.handlerResult(response, auth, product.getMarketplaceid());
		return Result.success();
	}
   
	@GetMapping("/loadformula")
	public Result<String> loadformulaAction() {
		UserInfo user = UserInfoContext.get();
		DaysalesFormula dayformula = daysalesFormulaService.selectByShopid(user.getCompanyid());
		if(dayformula!=null){
			return Result.success(dayformula.getFormulaName());
		}
		return Result.success(null);
	}
	
	@SystemControllerLog(value = "计算日均销量公式改变")
	@GetMapping("/formulaSave")
	public Result<String> formulaSaveAction(String formuladata) {
		UserInfo user = UserInfoContext.get();
		return Result.success(iProductInOptService.saveformulaData(user, formuladata));
	}
    
	@GetMapping(value = "/downExcelMSKUData")
	public void downExcelReturnsDateAction(String groupid,String marketplaceid, HttpServletResponse response)  {
		// 创建新的Excel工作薄
		SXSSFWorkbook workbook = new SXSSFWorkbook();
		// 将数据写入Excel
		UserInfo user = UserInfoContext.get();
		Map<String, Object> param = new HashMap<String, Object>();
	 
		if(StrUtil.isNotEmpty(groupid)) {
			if("all".equals(groupid)||"".equals(groupid)) {
				List<String> groupList =user.getGroups();
				if(groupList!=null&&groupList.size()>0) {
					param.put("groupList", groupList);
				}
			}else {
				param.put("groupid", groupid);
			}
		}
		if(StrUtil.isNotBlank(marketplaceid)) {
			param.put("marketplaceid", marketplaceid);
		}
		param.put("shopid", user.getCompanyid());
		iProductInOptService.setExcelMskuBook(workbook, param);
		try {
			response.setContentType("application/force-download");// 设置强制下载不打开
			response.addHeader("Content-Disposition", "attachment;fileName=mskutemplate" + System.currentTimeMillis() + ".xlsx");// 设置文件名
			ServletOutputStream fOut = response.getOutputStream();
			workbook.write(fOut);
			workbook.close();
			fOut.flush();
			fOut.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@PostMapping(value = "/uploadMskuFile",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Result<String> uploadExcelAction(@RequestParam("file")MultipartFile file)  {
	       UserInfo user=UserInfoContext.get();
			if (file != null) {
				try {
					InputStream inputStream = file.getInputStream();
					Workbook workbook = WorkbookFactory.create(inputStream);
					Sheet sheet = workbook.getSheetAt(0);
					for (int i = 1; i <= sheet.getLastRowNum(); i++) {
						Row info=sheet.getRow(i);
						if(info==null || info.getCell(0)==null) {
							continue;
						}
						iProductInOptService.uploadMskuFile(user,inputStream, info);
					}
					workbook.close();
					return Result.success();
				} catch (IOException e) {
					e.printStackTrace();
					return Result.failed();
				} catch (EncryptedDocumentException e) {
					e.printStackTrace();
				} catch (InvalidFormatException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		return Result.success("ok");
	}
	
	@GetMapping(value = "/updateProductOwner")
	public Result<String> updateProductOwner(String msku,String owner,String oldowner){
		//通过msku去找对应的info
		UserInfo user=UserInfoContext.get();
		iProductInOptService.updateProductOwner(user,msku,owner,oldowner);
		return Result.success("ok");
	}
	
	@GetMapping(value="/findPriceListByPid")
	public Result<?> findPriceListByPidAction(String pid){
		//通过msku去找对应的info
		List<AmzProductPriceRecord> list = amzProductPriceRecordService.findPriceListByPid(pid);
		return Result.success(list);
	}
	
	
}

