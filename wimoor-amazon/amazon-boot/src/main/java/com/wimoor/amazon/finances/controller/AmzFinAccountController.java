package com.wimoor.amazon.finances.controller;


import java.util.Date;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wimoor.amazon.auth.pojo.entity.AmazonAuthority;
import com.wimoor.amazon.auth.service.IAmazonAuthorityService;
import com.wimoor.amazon.finances.service.IAmzFinAccountService;
import com.wimoor.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author wimoor team
 * @since 2022-06-22
 */
@Api(tags = "亚马逊财务接口")
@RestController
@Component("amzFinAccountController")
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/amzFinAccount")
public class AmzFinAccountController {
   final IAmzFinAccountService iAmzFinAccountService;
   final IAmazonAuthorityService iAmazonAuthorityService;
   
   @ApiOperation(value = "更新未出账账期")
   @GetMapping("/refreshAmzFin")
   public Result<?> refreshAmzFin() {
	log.info("更新未出账账期------"+new Date());
   	iAmazonAuthorityService.executTask(iAmzFinAccountService);
       return Result.judge(true);
   }
   
   @ApiOperation(value = "更新未出账账期")
   @GetMapping("/listFinancialEventsByGroupId")
   public Result<?> listFinancialEventsByGroupId(String authid,String groupid,String token) {
	   	   AmazonAuthority auth = iAmazonAuthorityService.getById(authid);
	       return Result.success(iAmzFinAccountService.listFinancialEventsByGroupId(auth, groupid, token));
   }
   
}

