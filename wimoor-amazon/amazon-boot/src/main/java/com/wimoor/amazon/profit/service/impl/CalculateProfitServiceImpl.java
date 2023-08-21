package com.wimoor.amazon.profit.service.impl;

import java.math.BigDecimal;

import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wimoor.amazon.api.ErpClientOneFeignManager;
import com.wimoor.amazon.auth.pojo.entity.AmazonAuthority;
import com.wimoor.amazon.auth.pojo.entity.Marketplace;
import com.wimoor.amazon.auth.service.IMarketplaceService;
import com.wimoor.amazon.common.mapper.DimensionsInfoMapper;
import com.wimoor.amazon.common.pojo.entity.DimensionsInfo;
import com.wimoor.amazon.finances.mapper.FBAEstimatedFeeMapper;
import com.wimoor.amazon.finances.pojo.entity.FBAEstimatedFee;
import com.wimoor.amazon.inbound.mapper.FBAShipCycleMapper;
import com.wimoor.amazon.inbound.pojo.entity.FBAShipCycle;
import com.wimoor.amazon.product.mapper.ProductCategoryMapper;
import com.wimoor.amazon.product.pojo.entity.ProductInOpt;
import com.wimoor.amazon.product.pojo.entity.ProductInfo;
import com.wimoor.amazon.product.service.IProductInOptService;
import com.wimoor.amazon.profit.pojo.entity.ProfitConfig;
import com.wimoor.amazon.profit.pojo.entity.ReferralFee;
import com.wimoor.amazon.profit.pojo.vo.CostDetail;
import com.wimoor.amazon.profit.pojo.vo.InputDimensions;
import com.wimoor.amazon.profit.service.ICalculateProfitService;
import com.wimoor.amazon.profit.service.IProfitCfgService;
import com.wimoor.amazon.profit.service.IProfitService;
import com.wimoor.amazon.profit.service.IReferralFeeService;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
 
@Service("calculateProfitService")  
@RequiredArgsConstructor
public class CalculateProfitServiceImpl implements ICalculateProfitService {
	@Resource
	IProfitCfgService profitCfgService;
	@Autowired
	IMarketplaceService marketplaceService;
	@Autowired
	IProductInOptService iProductInOptService;
	final FBAEstimatedFeeMapper fbaEstimatedFeeMapper;
	final ProductCategoryMapper productCategoryMapper;
	final IReferralFeeService referralFeeService;
	final IProfitService profitService;
	final FBAShipCycleMapper fBAShipCycleMapper;
	final DimensionsInfoMapper dimensionsInfoMapper;
    @Autowired
    ErpClientOneFeignManager erpClientOneFeign;
	public CostDetail getProfit(ProductInfo info, BigDecimal price, AmazonAuthority auth,DimensionsInfo dim_local,BigDecimal cost)   {
		if (price == null || price.floatValue() == 0.0) {
			return null;
		}
		//使用opt上面带的利润id(假如设置了)
		ProductInOpt proopt = iProductInOptService.getById(info.getId());
		// 获取用户输入信息
		InputDimensions inputDimension_local = null;
		InputDimensions inputDimension_amz = null;
       
		DimensionsInfo dim_amz =null;
		if(dim_local==null) {
			if(info!=null&&info.getPageDimensions()!=null) {
				dim_amz= dimensionsInfoMapper.selectById(info.getPageDimensions());
			}
		}else{
			dim_amz = dim_local;
		}
		if (dim_local != null) {
			inputDimension_local = dim_local.getInputDimensions();
			inputDimension_amz = dim_amz.getInputDimensions();
		} else {
			return null;
		}
	
		Marketplace market = marketplaceService.selectByPKey(info.getMarketplaceid());
		String country = market.getMarket();
		String profitcfgid=null;
	
		if(proopt!=null) {
			if(proopt.getProfitid()!=null) {
				profitcfgid=proopt.getProfitid().toString();
			}
		} 
		if(StrUtil.isEmpty(profitcfgid)) {
			profitcfgid = profitCfgService.findDefaultPlanIdByGroup(auth.getGroupid());
		}
		ProfitConfig profitcfg = profitCfgService.findConfigAction(profitcfgid);
		ReferralFee ref = new ReferralFee();
	
		
		FBAEstimatedFee fbaFee = null;
		LambdaQueryWrapper<FBAEstimatedFee> queryFbaFee=new LambdaQueryWrapper<FBAEstimatedFee>();
		queryFbaFee.eq(FBAEstimatedFee::getSku, info.getSku());
		queryFbaFee.eq(FBAEstimatedFee::getAsin, info.getAsin());
		queryFbaFee.eq(FBAEstimatedFee::getAmazonauthid, auth.getId());
		queryFbaFee.eq(FBAEstimatedFee::getMarketplaceid, market.getMarketplaceid());
		fbaFee = fbaEstimatedFeeMapper.selectOne(queryFbaFee);
	 
		if (fbaFee!=null && fbaFee.getProductGroup()!=null) {
			ref = referralFeeService.findByPgroup(fbaFee.getProductGroup().trim(), country);
		} else {
            if(ref==null||ref.getId()==null) {
            	ref = referralFeeService.findCommonOther(country);
            }
		}
		if(ref==null||ref.getId()==null) {
			return null;
		}
		int typeid = ref.getId();
		String type = ref.getType();
		String isMedia = this.profitService.isMedia(typeid);// 是否为媒介
		BigDecimal shipmentfee = null;
		FBAShipCycle stockCycle = null;
		if ("EU".equals(market.getRegion())) {
			stockCycle = fBAShipCycleMapper.findShipCycleBySKU(info.getSku(), "EU", auth.getGroupid());
		} else {
			stockCycle = fBAShipCycleMapper.findShipCycleBySKU(info.getSku(), market.getMarketplaceid(), auth.getGroupid());
		}
		if (stockCycle != null&&stockCycle.getFirstLegCharges()!=null) {
			shipmentfee = stockCycle.getFirstLegCharges();// 头程运费
		}
		CostDetail deatail = null;
		try {

			boolean inSnl = info.getInSnl() == null ? false : info.getInSnl();
			if (fbaFee == null) {
				deatail = this.profitService.getProfitByLocalData(country, profitcfg, inputDimension_amz,
						inputDimension_local, isMedia, type, typeid, cost, "RMB", price, "local", inSnl, shipmentfee);
			}else {
				if (fbaFee != null && deatail == null) {
					ref = referralFeeService.findByPgroup(fbaFee.getProductGroup(), country);
					deatail = this.profitService.getProfitByAmazonData(country, profitcfg, inputDimension_local, isMedia,
							cost, "RMB", price, fbaFee, ref, inSnl, shipmentfee);
				}
				deatail.setProductTier(fbaFee.getProductSizeTier());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}  
		return deatail;
	}
	
	 
}
