package com.wimoor.common.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

import com.wimoor.common.service.SystemSchedulerService;
import org.springframework.core.annotation.Order;


/**
 * 实现ApplicationRunner接口，在程序启动的时候就立即执行逻辑
 * 如果当前是生成模式，那么立即刷新任务表，开始获取亚马逊数据
 */
@Configuration
@Order(value = 1) //如果有多个run方法吗，优先级低的先执行
public class SystemSchedulerInit implements ApplicationRunner {

   @Value("${spring.profiles.active}")
   String profile;
   @Value("${spring.application.name}")
   String server;
   @Autowired
   SystemSchedulerService systemSchedulerService;
   
   public String getProfile() {
	return profile;
   }
 
	public String getServer() {
		return server;
	}

@Override
   public void run(ApplicationArguments args) throws Exception {
	   if("prod".equals(profile)) {
		    // systemSchedulerService.insertTask();
		    systemSchedulerService.refreshTask();
	   }
   }
}
 