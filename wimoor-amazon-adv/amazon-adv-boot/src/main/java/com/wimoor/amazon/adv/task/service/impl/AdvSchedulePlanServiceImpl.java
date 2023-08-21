package com.wimoor.amazon.adv.task.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import javax.annotation.Resource;

import org.quartz.CronExpression;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;

import com.github.miemiedev.mybatis.paginator.domain.PageBounds;
import com.github.miemiedev.mybatis.paginator.domain.PageList;
import com.wimoor.amazon.adv.common.pojo.AmzAdvProfile;
import com.wimoor.amazon.adv.common.pojo.BaseException;
import com.wimoor.amazon.adv.common.service.IAmzAdvAuthService;
import com.wimoor.amazon.adv.common.service.IAmzAdvTimeTaskHandlerService;
import com.wimoor.amazon.adv.common.service.impl.AmzAdvReportHandlerServiceImpl.AdvRecordType;
import com.wimoor.amazon.adv.task.dao.AmzAdvSchedulePlanDataMapper;
import com.wimoor.amazon.adv.task.dao.AmzAdvSchedulePlanItemMapper;
import com.wimoor.amazon.adv.task.dao.AmzAdvSchedulePlanMapper;
import com.wimoor.amazon.adv.task.pojo.AmzAdvSchedulePlan;
import com.wimoor.amazon.adv.task.pojo.AmzAdvSchedulePlanData;
import com.wimoor.amazon.adv.task.pojo.AmzAdvSchedulePlanItem;
import com.wimoor.amazon.adv.task.pojo.SysTimeTask;
import com.wimoor.amazon.adv.task.service.IAdvSchedulePlanItemService;
import com.wimoor.amazon.adv.task.service.IAdvSchedulePlanService;
import com.wimoor.amazon.adv.task.service.ISysTimeTaskService;
import com.wimoor.amazon.base.BaseService;
import com.wimoor.common.GeneralUtil;
import com.wimoor.common.user.UserInfo;

import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.entity.Example.Criteria;
import tk.mybatis.mapper.util.StringUtil;

@Service("advSchedulePlanService")
public class AdvSchedulePlanServiceImpl extends BaseService<AmzAdvSchedulePlan> implements IAdvSchedulePlanService {
	@Resource
	ISysTimeTaskService sysTimeTaskService;
	@Resource
	AmzAdvSchedulePlanMapper amzAdvSchedulePlanMapper;
	@Resource
	AmzAdvSchedulePlanItemMapper amzAdvSchedulePlanItemMapper;
	@Resource
	IAdvSchedulePlanItemService advSchedulePlanItemService;
	@Autowired
	private SchedulerFactoryBean schedulerFactoryBean;
	@Resource
	IAmzAdvTimeTaskHandlerService amzAdvTimeTaskHandlerService;
	@Resource
	AmzAdvSchedulePlanDataMapper amzAdvSchedulePlanDataMapper;
	@Resource
	IAmzAdvAuthService amzAdvAuthService;
	public Date getNextFireTime(String cronExpression, AmzAdvProfile  profile) {
		if (StringUtil.isEmpty(cronExpression)) {
			return null;
		}
		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    format.setTimeZone(TimeZone.getTimeZone(profile.getTimezone()));
			CronExpression cron = new CronExpression(cronExpression);
			Date nextFireDate = cron.getNextValidTimeAfter(new Date(System.currentTimeMillis()));
			String startDay = format.format(nextFireDate.getTime());
			return GeneralUtil.StringfromDate(startDay, "yyyy-MM-dd");
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<String> getCron(Map<String, Object> map) {
		String cron = null;
		String type = (String) map.get("type");
		String startTime = (String) map.get("startTime");
		String yearDay = (String) map.get("yearDay");
		BigInteger planid = (BigInteger) map.get("planId");
		String[] times = startTime.split(":");
		String[] monthDay = yearDay.split("/");
		if (monthDay != null && monthDay.length == 1) {
			monthDay = monthDay[0].split("-");
		}
		String profileId =map.get("profileid").toString();
		AmzAdvProfile profile = amzAdvAuthService.getAmzAdvProfileByKey(new BigInteger(profileId));
		List<String> list = new ArrayList<String>();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		int timezone = 0;
	    sdf.setTimeZone(TimeZone.getTimeZone(profile.getTimezone()));
		long time = new Date().getTime();
		long time2 = 0;
		String nowDate = GeneralUtil.formatDate(new Date(), "yyyy/MM/dd HH:mm:ss");
		try {
			Date date = sdf.parse(nowDate);
			time2 = date.getTime();
			timezone = (int) Math.round((time2 - time) / 3600000.0);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if ("one".equals(type)) {
			cron = times[2] + " " + times[1] + " " + times[0] + " " + monthDay[2] + " " + monthDay[1] + " ?";
			list.add(cron);
		} else {
			String weekdays = (String) map.get("weekdays");
			if ("everyDay".equals(weekdays)) {
				int hour = Integer.parseInt(times[0]) + timezone;
				if (hour >= 24) {
					hour = hour - 24;
				}
				if(hour < 0) {
					hour = hour * -1;
				}
				times[0] = "" + hour;
				cron = times[2] + " " + times[1] + " " + times[0] + " * * ?";
				list.add(cron);
			} else {
				int hour = Integer.parseInt(times[0]) + timezone;
				String startweek = null;
				if (hour >= 24) {
					hour = hour - 24;
					if ("2,3,4,5,6".equals(weekdays))
						startweek = "3,4,5,6,7";
					if ("7,1".equals(weekdays))
						startweek = "1,2";
					if ("2".equals(weekdays))
						startweek = "3";
					if ("3".equals(weekdays))
						startweek = "4";
					if ("4".equals(weekdays))
						startweek = "5";
					if ("5".equals(weekdays))
						startweek = "6";
					if ("6".equals(weekdays))
						startweek = "7";
					if ("7".equals(weekdays))
						startweek = "1";
					if ("1".equals(weekdays))
						startweek = "2";
				}
				if(hour < 0) {
					hour = hour * -1;
				}
				times[0] = "" + hour;
				if (startweek != null) {
					cron = times[2] + " " + times[1] + " " + times[0] + " ? * " + startweek;
				} else {
					cron = times[2] + " " + times[1] + " " + times[0] + " ? * " + weekdays;
				}
				list.add(cron);
			}
		}
		Date startDate = getNextFireTime(cron, profile);
		AmzAdvSchedulePlan entity = new AmzAdvSchedulePlan();
		entity.setId(planid);
		entity = super.selectByKey(entity);
		if(entity != null) {
			if(entity.getStartdate() != null) {
				if(entity.getStartdate().getTime() > startDate.getTime()) {
					entity.setStartdate(startDate);
				}
			}else {
				entity.setStartdate(startDate);
			}
			super.updateNotNull(entity);
		}
		return list;
	}

	private int saveMyTimeTask(Map<String, Object> map) {
		List<String> list = new ArrayList<String>();
		int temp = 0;
		String cron = null;
		String jobData = null;
		String userId = (String) map.get("userId");
		String taskId = (String) map.get("taskId");
		jobData = (String) map.get("newjobData");
		list = getCron(map);
		for (int i = 0; i < list.size(); i++) {
			cron = list.get(i);
			SysTimeTaskServiceImpl.checkparam(cron, jobData);
			SysTimeTask record = new SysTimeTask();
			record.setId(taskId);
			record.setJobData(jobData);
			record.setName("广告任务");
			record.setGroupName("广告活动" + UUID.randomUUID().toString());
			record.setStartTime(new Date());
			record.setEndTime(new Date());
			record.setCron(cron);
			record.setJobStatus("1");
			record.setIsConcurrent(1);
			record.setPlanStatus("1");
			record.setMenthodName("timeTask");
			record.setBeanName("amzAdvTimeTaskHandlerService");
			record.setCreator(userId);
			record.setCreatetime(new Date());
			temp = temp + sysTimeTaskService.saveTimeTask(record);
		}
		return temp;
	}

	public String getJobData(Map<String, Object> map) {
		String recordType = (String) map.get("recordType");
		String campaignType = (String) map.get("campaignType");
		String profileId = map.get("profileid").toString();
		String campaignId = (String) map.get("campaignId");
		String adGroupId = (String) map.get("adGroupId");
		String adId = (String) map.get("adId");
		String keywordId = (String) map.get("keywordId");
		String planId = map.get("planId").toString();
		String taskId = (String) map.get("taskId");
		String type = (String) map.get("type");
		String country = (String) map.get("country");
		String status = (String) map.get("mystatus");
		String bid = (String) map.get("mybid");
		StringBuffer jobbuffer = new StringBuffer();
		jobbuffer.append("{\"recordType\":\"" + recordType + "\",\"country\":\"" + country + "\",\"campaignType\":\""
				+ campaignType + "\",\"profileId\":\"" + profileId + "\",\"planId\":\"" + planId + "\",\"status\":\"" 
				+ status + "\",\"bid\":\"" + bid + "\"");
		if (!"one".equals(type)) {
			jobbuffer.append(",\"planStatus\":\"executing\",\"type\":\"" + type + "\",\"taskId\":\"" + taskId + "\"");
		} else {
			jobbuffer.append(",\"planStatus\":\"completed\",\"type\":\"" + type + "\",\"taskId\":\"" + taskId + "\"");
		}
		if (campaignId != null) {
			jobbuffer.append(",\"campaignId\":\"" + campaignId + "\"");
		}
		if (adGroupId != null) {
			jobbuffer.append(",\"adGroupId\":\"" + adGroupId + "\"");
		}
		if (adId != null) {
			String[] adIdArray = adId.split(",");
			jobbuffer.append(",\"adIdArray\":[");
			for (int i = 0; i < adIdArray.length; i++) {
				jobbuffer.append("{\"adId\":\"");
				jobbuffer.append(adIdArray[i] + "\"");
				if (i == adIdArray.length - 1) {
					jobbuffer.append("}]");
				} else {
					jobbuffer.append("},");
				}
			}
		}
		if (keywordId != null) {
			String[] keywordIdArray = keywordId.split(",");
			jobbuffer.append(",\"keywordIdArray\":[");
			for (int i = 0; i < keywordIdArray.length; i++) {
				jobbuffer.append("{\"keywordId\":\"");
				jobbuffer.append(keywordIdArray[i] + "\"");
				if (i == keywordIdArray.length - 1) {
					jobbuffer.append("}]");
				} else {
					jobbuffer.append("},");
				}
			}
		}
		jobbuffer.append("}");
		return jobbuffer.toString();
	}

	public String getOldJobData(Map<String, Object> map) {
		String campaignId = (String) map.get("campaignId");
		String adGroupId = (String) map.get("adGroupId");
		String adId = (String) map.get("adId");
		String keywordId = (String) map.get("keywordId");
		String recordType = (String) map.get("recordType");
		String campaignType = (String) map.get("campaignType");
		String profileId = map.get("profileid").toString();
		String planId = map.get("planId").toString();
		String secondTaskId = (String) map.get("secondTaskId");
		String country = (String) map.get("country");
		StringBuffer jobbuffer = new StringBuffer();
		jobbuffer.append("{\"recordType\":\"" + recordType + "\",\"campaignType\":\"" + campaignType
				+ "\",\"country\":\"" + country + "\",\"profileId\":\"" + profileId + "\",\"planId\":\"" + planId
				+ "\",\"planStatus\":\"wait\",\"type\":\"cycle\"" + ",\"secondTaskId\":\"" + secondTaskId + "\"");
		if (AdvRecordType.campaigns.equals(recordType)) {
			// jobbuffer.append(",\"campaignId\":\"" + campaignId + "\",\"status\":\"" +
			// status + "\",\"bid\":\"" + bid + "\"");
			jobbuffer.append(",\"campaignId\":\"" + campaignId + "\"");
		} else if (AdvRecordType.adGroups.equals(recordType)) {
			jobbuffer.append(",\"campaignId\":\"" + campaignId + "\"");
			jobbuffer.append(",\"adGroupId\":\"" + adGroupId + "\"");
		} else if (AdvRecordType.productAds.equals(recordType)) {
			jobbuffer.append(",\"campaignId\":\"" + campaignId + "\"");
			jobbuffer.append(",\"adGroupId\":\"" + adGroupId + "\"");
			String[] adIdArray = adId.split(",");
			jobbuffer.append(",\"adIdArray\":[");
			for (int i = 0; i < adIdArray.length; i++) {
				jobbuffer.append("{\"adId\":\"" + adIdArray[i] + "\"");
				if (i == adIdArray.length - 1) {
					jobbuffer.append("}]");
				} else {
					jobbuffer.append("},");
				}
			}
		} else if (AdvRecordType.keywords.equals(recordType)) {
			jobbuffer.append(",\"campaignId\":\"" + campaignId + "\"");
			jobbuffer.append(",\"adGroupId\":\"" + adGroupId + "\"");
			String[] keywordIdArray = keywordId.split(",");
			StringBuffer keywordbuffer = new StringBuffer();
			for (int i = 0; i < keywordIdArray.length; i++) {
				keywordbuffer.append("{\"keywordId\":\"" + keywordIdArray[i] + "\"");
				if (i == keywordIdArray.length - 1) {
					keywordbuffer.append("}]");
				} else {
					keywordbuffer.append("},");
				}
			}
			jobbuffer.append(",\"keywordIdArray\":[");
			jobbuffer.append(keywordbuffer.toString());
		} else if (AdvRecordType.negativeKeywords.equals(recordType)) {
			jobbuffer.append(",\"campaignId\":\"" + campaignId + "\"");
			jobbuffer.append(",\"adGroupId\":\"" + adGroupId + "\"");
			String[] keywordIdArray = keywordId.split(",");
			jobbuffer.append(",\"keywordIdArray\":[");
			for (int i = 0; i < keywordIdArray.length; i++) {
				jobbuffer.append("{\"keywordId\":\"");
				jobbuffer.append(keywordIdArray[i] + "\"");
				if (i == keywordIdArray.length - 1) {
					jobbuffer.append("}]");
				} else {
					jobbuffer.append("},");
				}
			}
		}
		jobbuffer.append("}");
		return jobbuffer.toString();
	}
	
	

	private void insertPlanDate(List<AmzAdvSchedulePlanData> plandataList, BigInteger id) {
		Example example=new Example(AmzAdvSchedulePlanData.class);
		Criteria crit = example.createCriteria();
		crit.andEqualTo("planid",id);
		amzAdvSchedulePlanDataMapper.deleteByExample(example);
		for(AmzAdvSchedulePlanData plandata:plandataList) {
			plandata.setPlanid(id);
			amzAdvSchedulePlanDataMapper.insertUseGeneratedKeys(plandata);
		}
	}
	
	public String getWeeks(String[] weeks, String[] ranges) {
		String conditionsexecute = "";
		String everyDaydatetime = "";
		String datetime23456 = "";
		String datetime71 = "";
		String datetime1 = "";
		String datetime2 = "";
		String datetime3 = "";
		String datetime4 = "";
		String datetime5 = "";
		String datetime6 = "";
		String datetime7 = "";
		for(int j = 0;j < weeks.length; j++) {
			String weekdayname = weeks[j];
			if ("everyDay".equals(weekdayname)) {
				if("".equals(everyDaydatetime)) {
					everyDaydatetime = "每天(时分)" + "," + ranges[j];
				}else {
					everyDaydatetime = everyDaydatetime + "," + ranges[j];
				}
			}
			if ("2,3,4,5,6".equals(weekdayname)) {
				if("".equals(datetime23456)) {
					datetime23456 = "周一到周五(时分)" + "," + ranges[j];
				}else {
					datetime23456 = datetime23456 + "," + ranges[j];
				}
			}
			if ("7,1".equals(weekdayname)) {
				if("".equals(datetime71)) {
					datetime71 = "周六到周日(时分)" + "," + ranges[j];
				}else {
					datetime71 = datetime71 + "," + ranges[j];
				}
			}
			if ("2".equals(weekdayname)) {
				if("".equals(datetime2)) {
					datetime2 = "周一(时分)" + "," + ranges[j];
				}else {
					datetime2 = datetime2 + "," + ranges[j];
				}
			}
			if ("3".equals(weekdayname)) {
				if("".equals(datetime3)) {
					datetime3 = "周二(时分)" + "," + ranges[j];
				}else {
					datetime3 = datetime3 + "," + ranges[j];
				}
			}
			if ("4".equals(weekdayname)) {
				if("".equals(datetime4)) {
					datetime4 = "周三(时分)" + "," + ranges[j];
				}else {
					datetime4 = datetime4 + "," + ranges[j];
				}
			}
			if ("5".equals(weekdayname)) {
				if("".equals(datetime5)) {
					datetime5 = "周四(时分)" + "," + ranges[j];
				}else {
					datetime5 = datetime5 + "," + ranges[j];
				}
			}
			if ("6".equals(weekdayname)) {
				if("".equals(datetime6)) {
					datetime6 = "周五(时分)" + "," + ranges[j];
				}else {
					datetime6 = datetime6 + "," + ranges[j];
				}
			}
			if ("7".equals(weekdayname)) {
				if("".equals(datetime7)) {
					datetime7 = "周六(时分)" + "," + ranges[j];
				}else {
					datetime7 = datetime7 + "," + ranges[j];
				}
			}
			if ("1".equals(weekdayname)) {
				if("".equals(datetime1)) {
					datetime1 = "周日(时分)" + "," + ranges[j];
				}else {
					datetime1 = datetime1 + "," + ranges[j];
				}
			}
		}
		if(!"".equals(everyDaydatetime)) {
			conditionsexecute = everyDaydatetime;
		}
		if(!"".equals(datetime23456)) {
			if("".equals(conditionsexecute)) {
				conditionsexecute = datetime23456;
			}else {
				conditionsexecute = conditionsexecute + "," + datetime23456;
			}
		}
		if(!"".equals(datetime71)) {
			if("".equals(conditionsexecute)) {
				conditionsexecute = datetime71;
			}else {
				conditionsexecute = conditionsexecute + "," + datetime71;
			}
		}
		if(!"".equals(datetime2)) {
			if("".equals(conditionsexecute)) {
				conditionsexecute = datetime2;
			}else {
				conditionsexecute = conditionsexecute + "," + datetime2;
			}
		}
		if(!"".equals(datetime3)) {
			if("".equals(conditionsexecute)) {
				conditionsexecute = datetime3;
			}else {
				conditionsexecute = conditionsexecute + "," + datetime3;
			}
		}
		if(!"".equals(datetime4)) {
			if("".equals(conditionsexecute)) {
				conditionsexecute = datetime4;
			}else {
				conditionsexecute = conditionsexecute + "," + datetime4;
			}
		}
		if(!"".equals(datetime5)) {
			if("".equals(conditionsexecute)) {
				conditionsexecute = datetime5;
			}else {
				conditionsexecute = conditionsexecute + "," + datetime5;
			}
		}
		if(!"".equals(datetime6)) {
			if("".equals(conditionsexecute)) {
				conditionsexecute = datetime6;
			}else {
				conditionsexecute = conditionsexecute + "," + datetime6;
			}
		}
		if(!"".equals(datetime7)) {
			if("".equals(conditionsexecute)) {
				conditionsexecute = datetime7;
			}else {
				conditionsexecute = conditionsexecute + "," + datetime7;
			}
		}
		if(!"".equals(datetime1)) {
			if("".equals(conditionsexecute)) {
				conditionsexecute = datetime1;
			}else {
				conditionsexecute = conditionsexecute + "," + datetime1;
			}
		}
		return conditionsexecute + "," + "周期性执行";
	}
	
	public List<AmzAdvSchedulePlanData> getPlandataList(Map<String, Object> map) {
		String campaignId =map.containsKey("campaignId")? (String) map.get("campaignId"):null;
		String adGroupId = map.containsKey("adGroupId")?(String) map.get("adGroupId"):null;
		String adId = map.containsKey("adId")?(String) map.get("adId"):null;
		String keywordId = map.containsKey("keywordId")?(String) map.get("keywordId"):null;
		String userId = map.containsKey("userId") ? (String) map.get("userId") : null;
		BigInteger profileid =map.containsKey("profileid")? new BigInteger(map.get("profileid").toString()):null;
		List<AmzAdvSchedulePlanData> plandataList = new ArrayList<AmzAdvSchedulePlanData>();
		if (adId != null) {
			String[] adIdArray = adId.split(",");
			for(String myadId:adIdArray) {
				if(StringUtil.isEmpty(myadId))continue;
				AmzAdvSchedulePlanData azAdvSchedulePlanData = new AmzAdvSchedulePlanData();
				azAdvSchedulePlanData.setCampaignid(new BigInteger(campaignId));
				azAdvSchedulePlanData.setAdgroupid(new BigInteger(adGroupId));
				azAdvSchedulePlanData.setProfileid(profileid);
				azAdvSchedulePlanData.setOldbid(null);
				azAdvSchedulePlanData.setOldstatus(null);
				azAdvSchedulePlanData.setOperator(userId);
				azAdvSchedulePlanData.setOpttime(new Date());
				azAdvSchedulePlanData.setAdid(new BigInteger(myadId));
				azAdvSchedulePlanData.setKeywordid(new BigInteger("0"));
				plandataList.add(azAdvSchedulePlanData);
			}
		}
		else if (keywordId != null) {
			String[] keywordIdArray = keywordId.split(",");
			for(String mykeywordId:keywordIdArray) {
				if(StringUtil.isEmpty(mykeywordId))continue;
				AmzAdvSchedulePlanData azAdvSchedulePlanData = new AmzAdvSchedulePlanData();
				azAdvSchedulePlanData.setCampaignid(new BigInteger(campaignId));
				azAdvSchedulePlanData.setAdgroupid(new BigInteger(adGroupId));
				azAdvSchedulePlanData.setProfileid(profileid);
				azAdvSchedulePlanData.setOldbid(null);
				azAdvSchedulePlanData.setOldstatus(null);
				azAdvSchedulePlanData.setOperator(userId);
				azAdvSchedulePlanData.setOpttime(new Date());
				azAdvSchedulePlanData.setKeywordid(new BigInteger(mykeywordId));
				azAdvSchedulePlanData.setAdid(new BigInteger("0"));
				plandataList.add(azAdvSchedulePlanData);
			}
		}
		else if(StringUtil.isNotEmpty(adGroupId)) {
			AmzAdvSchedulePlanData azAdvSchedulePlanData = new AmzAdvSchedulePlanData();
			azAdvSchedulePlanData.setCampaignid(new BigInteger(campaignId));
			azAdvSchedulePlanData.setAdgroupid(new BigInteger(adGroupId));
			azAdvSchedulePlanData.setProfileid(profileid);
			azAdvSchedulePlanData.setOldbid(null);
			azAdvSchedulePlanData.setOldstatus(null);
			azAdvSchedulePlanData.setOperator(userId);
			azAdvSchedulePlanData.setOpttime(new Date());
			azAdvSchedulePlanData.setKeywordid(new BigInteger("0"));
			azAdvSchedulePlanData.setAdid(new BigInteger("0"));
			plandataList.add(azAdvSchedulePlanData);
		}else {
			AmzAdvSchedulePlanData azAdvSchedulePlanData = new AmzAdvSchedulePlanData();
			azAdvSchedulePlanData.setCampaignid(new BigInteger(campaignId));
			azAdvSchedulePlanData.setAdgroupid(new BigInteger("0"));
			azAdvSchedulePlanData.setProfileid(profileid);
			azAdvSchedulePlanData.setOldbid(null);
			azAdvSchedulePlanData.setOldstatus(null);
			azAdvSchedulePlanData.setOperator(userId);
			azAdvSchedulePlanData.setOpttime(new Date());
			azAdvSchedulePlanData.setKeywordid(new BigInteger("0"));
			azAdvSchedulePlanData.setAdid(new BigInteger("0"));
			plandataList.add(azAdvSchedulePlanData);
		}
		return plandataList;
	}
	
	public int insertPlan(Map<String, Object> map) {
		String taskName = map.containsKey("taskName") ? (String) map.get("taskName") : null;
		String type = map.containsKey("type") ? (String) map.get("type") : null;
		String weekdays = map.containsKey("weekdays") ? (String) map.get("weekdays") : null;
		String startDatestr = map.containsKey("startDatestr") ? (String) map.get("startDatestr") : null;
		String bid = map.containsKey("bid") ? (String) map.get("bid") : null;
		String status = map.containsKey("status") ? (String) map.get("status") : null;
		String range = map.containsKey("range") ? (String) map.get("range") : null;
		String remark = map.containsKey("remark") ? (String) map.get("remark") : null;
		String country = map.containsKey("country") ? (String) map.get("country") : null;
		String userId = map.containsKey("userId") ? (String) map.get("userId") : null;
		
		List<AmzAdvSchedulePlanData> plandataList = getPlandataList(map);
		BigInteger profileid=null;
		if(plandataList != null && plandataList.size() == 0) {
			throw new BaseException("请选择具体的操作类型以及广告数据！");
		}else {
			profileid=plandataList.get(0).getProfileid();
		}
		AmzAdvProfile advprofile = amzAdvAuthService.getAmzAdvProfileByKey(profileid);
		int temp = 0;
		Date startDate = null;
		Date startDate2 = null;
		String yearDay = null;
		String startTime = null;
		String conditionsexecute = null;
		AmzAdvSchedulePlan advplan = new AmzAdvSchedulePlan();
		int random = (int) (Math.random() * 60);
		String second = "";
		if (random < 10) {
			second = ":0" + random;
		} else {
			second = ":" + random;
		}
		if ("one".equals(type)) {
			AmzAdvSchedulePlanItem advplanItem = new AmzAdvSchedulePlanItem();
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			if (startDatestr != null) {
				startDatestr = startDatestr + second;
				conditionsexecute = startDatestr + ",单次执行";
				startDate2 = GeneralUtil.StringfromDate(startDatestr, "yyyy-MM-dd HH:mm:ss");
		        format.setTimeZone(TimeZone.getTimeZone(advprofile.getTimezone()));
				try {
					startDate = format.parse(startDatestr);
					startDatestr = GeneralUtil.formatDate(startDate, "yyyy-MM-dd HH:mm:ss");
				} catch (ParseException e) {
					e.printStackTrace();
				}
				if(startDate.getTime() < new Date().getTime()) {
					throw new BaseException("执行时间小于所对应地区当前时间！");
				}
				yearDay = startDatestr.split(" ")[0];
				startTime = startDatestr.split(" ")[1];
			}
			advplan.setTaskname(taskName);
			advplan.setStatus("enabled");
			advplan.setConditionsexecute(conditionsexecute);
			advplan.setExecutestatus("wait");
			advplan.setStartdate(startDate2);
			advplan.setRemark(remark);
			advplan.setCreator(userId);
			advplan.setOpttime(new Date());
			advplan.setCreatetime(new Date());
			temp += this.insertUseGeneratedKeys(advplan);
			insertPlanDate(plandataList, advplan.getId());

			advplanItem.setTaskid(UUID.randomUUID().toString());
			advplanItem.setPlanid(advplan.getId());
			if(StringUtil.isNotEmpty(status)) {
				advplanItem.setStatus(status);
			}
			if(StringUtil.isNotEmpty(bid) && !"-".equals(bid)) {
				advplanItem.setBid(new BigDecimal(bid));
			}else {
				advplanItem.setBid(null);
			}
			advplanItem.setType(type);
			advplanItem.setStarttime(GeneralUtil.StringfromDate(startTime, "HH:mm:ss"));
			temp += amzAdvSchedulePlanItemMapper.insert(advplanItem);

			map.put("taskId", advplanItem.getTaskid());
			map.put("planId", advplan.getId());
			map.put("startTime", startTime);
			map.put("yearDay", yearDay);
			map.put("mystatus", status);
			map.put("mybid", bid);
			map.put("profileid",profileid);
			map.put("newjobData", getJobData(map));
			temp = saveMyTimeTask(map);
		} else {
			if (range != null) {
				String[] ranges = range.split(",");
				String[] statuss = status.split(",");
				String[] bids = bid.split(",");
				String[] weeks = weekdays.split("#");
				conditionsexecute = getWeeks(weeks,ranges);
				advplan.setTaskname(taskName);
				advplan.setStatus("enabled");
				advplan.setConditionsexecute(conditionsexecute);
				advplan.setExecutestatus("wait");
				advplan.setRemark(remark);
				advplan.setCreator(userId);
				advplan.setOpttime(new Date());
				advplan.setCreatetime(new Date());
				temp += super.insertUseGeneratedKeys(advplan);
				insertPlanDate(plandataList, advplan.getId());
				
				for (int i = 0; i < ranges.length; i++) {
					AmzAdvSchedulePlanItem advplanItem = new AmzAdvSchedulePlanItem();
					if (StringUtil.isEmpty(ranges[i]))
						continue;
					String mystatu = statuss[i];
					String mybid = bids[i];
					startDatestr = ranges[i];
					startDatestr = startDatestr + second;
					startDate = GeneralUtil.StringfromDate(startDatestr, "HH:mm:ss");
					yearDay = "";
					startTime = startDatestr;
					advplanItem.setTaskid(UUID.randomUUID().toString());
					advplanItem.setPlanid(advplan.getId());
					if(StringUtil.isNotEmpty(mystatu)) {
						advplanItem.setStatus(mystatu);
					}
					if(StringUtil.isNotEmpty(mybid) && !"-".equals(mybid)) {
						advplanItem.setBid(new BigDecimal(mybid));
					}else {
						advplanItem.setBid(null);
					}
					advplanItem.setType(type);
					advplanItem.setWeekdays(weeks[i]);
					advplanItem.setStarttime(GeneralUtil.StringfromDate(startTime, "HH:mm:ss"));
					temp += amzAdvSchedulePlanItemMapper.insert(advplanItem);

					map.put("taskId", advplanItem.getTaskid());
					map.put("planId", advplan.getId());
					map.put("mystatus", mystatu);
					map.put("mybid", mybid);
					map.put("startTime", startTime);
					map.put("yearDay", yearDay);
					map.put("weekdays", weeks[i]);
					map.put("profileid",profileid);
					map.put("newjobData", getJobData(map));
					temp += saveMyTimeTask(map);
				}
			}
		}
		return temp;
	}

	public int updatePlan(Map<String, Object> map) {
		String taskName = map.containsKey("taskName") ? (String) map.get("taskName") : null;
		String type = map.containsKey("type") ? (String) map.get("type") : null;
		String weekdays = map.containsKey("weekdays") ? (String) map.get("weekdays") : null;
		String startDatestr = map.containsKey("startDatestr") ? (String) map.get("startDatestr") : null;
		String bid = map.containsKey("bid") ? (String) map.get("bid") : null;
		String status = map.containsKey("status") ? (String) map.get("status") : null;
		String range = map.containsKey("range") ? (String) map.get("range") : null;
		String remark = map.containsKey("remark") ? (String) map.get("remark") : null;
		String country = map.containsKey("country") ? (String) map.get("country") : null;
		String userId = map.containsKey("userId") ?   map.get("userId").toString() : null;
		String planId = (String) map.get("planId");
		
		List<AmzAdvSchedulePlanData> plandataList = getPlandataList(map);
		BigInteger profileid=null;
		if(plandataList != null && plandataList.size() == 0) {
			throw new BaseException("请选择具体的操作类型以及广告数据！");
		}else {
			profileid=plandataList.get(0).getProfileid();
		}
		AmzAdvProfile advprofile = amzAdvAuthService.getAmzAdvProfileByKey(profileid);
		int temp = 0;
		Date startDate = null;
		Date startDate2 = null;
		String yearDay = null;
		String startTime = null;
		String conditionsexecute = null;
		AmzAdvSchedulePlan advplan = amzAdvSchedulePlanMapper.selectByPrimaryKey(planId);
		Example example = new Example(AmzAdvSchedulePlanItem.class);
		Criteria crit = example.createCriteria();
		crit.andEqualTo("planid", planId);
		List<AmzAdvSchedulePlanItem> list = amzAdvSchedulePlanItemMapper.selectByExample(example);
		AmzAdvSchedulePlanItem amzAdvSchedulePlanItem = null;
		if (list != null && list.size() > 0) {
			for (int i = 0; i < list.size(); i++) {
				amzAdvSchedulePlanItem = list.get(i);
				String taskId = amzAdvSchedulePlanItem.getTaskid();
				sysTimeTaskService.deleteTimeTask(taskId);
				amzAdvSchedulePlanItemMapper.delete(amzAdvSchedulePlanItem);
			}
		}
		int random = (int) (Math.random() * 60);
		String second = "";
		if (random < 10) {
			second = ":0" + random;
		} else {
			second = ":" + random;
		}
		if ("one".equals(type)) {
			AmzAdvSchedulePlanItem advplanItem = new AmzAdvSchedulePlanItem();
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			if (startDatestr != null) {
				startDatestr = startDatestr + second;
				conditionsexecute = startDatestr + ",单次执行";
				startDate2 = GeneralUtil.StringfromDate(startDatestr, "yyyy-MM-dd HH:mm:ss");
			    format.setTimeZone(TimeZone.getTimeZone(advprofile.getTimezone()));
				try {
					startDate = format.parse(startDatestr);
					startDatestr = GeneralUtil.formatDate(startDate, "yyyy-MM-dd HH:mm:ss");
				} catch (ParseException e) {
					e.printStackTrace();
				}
				if(startDate.getTime() < new Date().getTime()) {
					throw new BaseException("执行时间小于所对应地区当前时间！");
				}
				yearDay = startDatestr.split(" ")[0];
				startTime = startDatestr.split(" ")[1];
			}
			advplan.setTaskname(taskName);
			advplan.setStatus("enabled");
			advplan.setConditionsexecute(conditionsexecute);
			advplan.setExecutestatus("wait");
			advplan.setStartdate(startDate2);
			advplan.setRemark(remark);
			advplan.setCreator(userId);
			advplan.setOpttime(new Date());
			advplan.setCreatetime(new Date());
			temp += this.updateNotNull(advplan);
			insertPlanDate(plandataList, advplan.getId());

			advplanItem.setTaskid(UUID.randomUUID().toString());
			advplanItem.setPlanid(advplan.getId());
			if(StringUtil.isNotEmpty(status)) {
				advplanItem.setStatus(status);
			}
			if(StringUtil.isNotEmpty(bid) && !"-".equals(bid)) {
				advplanItem.setBid(new BigDecimal(bid));
			}else {
				advplanItem.setBid(null);
			}
			advplanItem.setType(type);
			advplanItem.setStarttime(GeneralUtil.StringfromDate(startTime, "HH:mm:ss"));
			temp += amzAdvSchedulePlanItemMapper.insert(advplanItem);

			map.put("taskId", advplanItem.getTaskid());
			map.put("planId", advplan.getId());
			map.put("startTime", startTime);
			map.put("yearDay", yearDay);
			map.put("mystatus", status);
			map.put("mybid", bid);
			map.put("newjobData", getJobData(map));
			temp = saveMyTimeTask(map);
		} else {
			if (range != null) {
				String[] ranges = range.split(",");
				String[] statuss = status.split(",");
				String[] bids = bid.split(",");
				String[] weeks = weekdays.split("#");
				conditionsexecute = getWeeks(weeks,ranges);
				advplan.setTaskname(taskName);
				advplan.setStatus("enabled");
				advplan.setConditionsexecute(conditionsexecute);
				advplan.setExecutestatus("wait");
				advplan.setRemark(remark);
				advplan.setCreator(userId);
				advplan.setOpttime(new Date());
				advplan.setCreatetime(new Date());
				temp += super.updateNotNull(advplan);
				insertPlanDate(plandataList, advplan.getId());
				
				for (int i = 0; i < ranges.length; i++) {
					AmzAdvSchedulePlanItem advplanItem = new AmzAdvSchedulePlanItem();
					if (StringUtil.isEmpty(ranges[i]))
						continue;
					String mystatu = statuss[i];
					String mybid = bids[i];
					startDatestr = ranges[i];
					startDatestr = startDatestr + second;
					startDate = GeneralUtil.StringfromDate(startDatestr, "HH:mm:ss");
					yearDay = "";
					startTime = startDatestr;
					advplanItem.setTaskid(UUID.randomUUID().toString());
					advplanItem.setPlanid(advplan.getId());
					if(StringUtil.isNotEmpty(mystatu)) {
						advplanItem.setStatus(mystatu);
					}
					if(StringUtil.isNotEmpty(mybid) && !"-".equals(mybid)) {
						advplanItem.setBid(new BigDecimal(mybid));
					}else {
						advplanItem.setBid(null);
					}
					advplanItem.setType(type);
					advplanItem.setWeekdays(weeks[i]);
					advplanItem.setStarttime(GeneralUtil.StringfromDate(startTime, "HH:mm:ss"));
					temp += amzAdvSchedulePlanItemMapper.insert(advplanItem);

					map.put("taskId", advplanItem.getTaskid());
					map.put("planId", advplan.getId());
					map.put("mystatus", mystatu);
					map.put("mybid", mybid);
					map.put("startTime", startTime);
					map.put("yearDay", yearDay);
					map.put("weekdays", weeks[i]);
					map.put("newjobData", getJobData(map));
					temp += saveMyTimeTask(map);
				}
			}
		}
		return temp;
	}

	public void updatePlanStatus(Map<String, Object> map) {
		// TODO Auto-generated method stub
		String planId = (String) map.get("planId");
		String status = (String) map.get("status");
		String[] planIdArray = planId.split(",");
		for (int j = 0; j < planIdArray.length; j++) {
			String id = planIdArray[j];
			AmzAdvSchedulePlan amzAdvSchedulePlan = amzAdvSchedulePlanMapper.selectByPrimaryKey(id);
			amzAdvSchedulePlan.setStatus(status);
			Example example = new Example(AmzAdvSchedulePlanItem.class);
			Criteria crit = example.createCriteria();
			crit.andEqualTo("planid", id);
			List<AmzAdvSchedulePlanItem> list = amzAdvSchedulePlanItemMapper.selectByExample(example);
			SysTimeTask record = null;
			AmzAdvSchedulePlanItem amzAdvSchedulePlanItem = null;
			if (list != null && list.size() > 0) {
				for (int i = 0; i < list.size(); i++) {
					amzAdvSchedulePlanItem = list.get(i);
					String taskId = amzAdvSchedulePlanItem.getTaskid();
					record = sysTimeTaskService.selectByKey(taskId);
					Scheduler scheduler = schedulerFactoryBean.getScheduler();
					JobKey jobKey = JobKey.jobKey(record.getName(), record.getGroupName());
					try {
						if ("paused".equals(status)) {
							if("0".equals(record.getJobStatus())) {
								throw new BaseException("任务计划已经停用,不能再次启动!");
							}
							scheduler.pauseJob(jobKey);
							record.setIsConcurrent(0);
							amzAdvSchedulePlan.setExecutestatus("wait");
						} else if ("enabled".equals(status)) {
							if("0".equals(record.getJobStatus())) {
								throw new BaseException("任务计划已经停用,不能再次暂停!");
							}
							scheduler.resumeJob(jobKey);
							record.setIsConcurrent(1);
							// amzAdvTimeTaskHandlerService.pausedTask(record.getJobData());
						} else if ("disable".equals(status)) {
							scheduler.deleteJob(jobKey);
							record.setJobStatus("0");
							amzAdvSchedulePlan.setExecutestatus("wait");
						}
						sysTimeTaskService.updateNotNull(record);
					} catch (SchedulerException e1) {
						e1.printStackTrace();
						throw new BaseException("planID:" + id + "系统异常导致任务计划仍在执行,请联系管理员!");
					}
				}
			}
			amzAdvSchedulePlan.setOpttime(new Date());
			this.updateNotNull(amzAdvSchedulePlan);
		}
	}

	public PageList<Map<String, Object>> getSchedulePlan(Map<String, Object> map, PageBounds pageBounds) {
		return amzAdvSchedulePlanMapper.getSchedulePlan(map, pageBounds);
	}

	public void updatePlanRemark(Map<String, Object> map) {
		// TODO Auto-generated method stub
		String planIdArray = (String) map.get("planId");
		String remark = (String) map.get("remark");
		String[] planId = planIdArray.split(",");
		for (int i = 0; i < planId.length; i++) {
			if (StringUtil.isEmpty(planId[i]))
				continue;
			AmzAdvSchedulePlan record = amzAdvSchedulePlanMapper.selectByPrimaryKey(planId[i]);
			record.setRemark(remark);
			record.setOpttime(new Date());
			this.updateNotNull(record);
		}
	}

	public Object getSchedulePlanInfo(UserInfo user, String planid) {
		// TODO Auto-generated method stub
		Map<String, Object> resultMap = new HashMap<String, Object>();
		AmzAdvSchedulePlan record = amzAdvSchedulePlanMapper.selectByPrimaryKey(planid);
		List<Map<String, Object>> subitem = amzAdvSchedulePlanMapper.selectSubitem(planid);
		List<Map<String, Object>> planData = amzAdvSchedulePlanMapper.selectPlanData(planid);
		resultMap.put("planinfo", record);
		resultMap.put("planData", planData);
		resultMap.put("subitem", subitem);
		return resultMap;
	}

	public void deletePlan(String planId) {
		Example example = new Example(AmzAdvSchedulePlanItem.class);
		Criteria crit = example.createCriteria();
		crit.andEqualTo("planid", planId);
		List<AmzAdvSchedulePlanItem> list = amzAdvSchedulePlanItemMapper.selectByExample(example);
		AmzAdvSchedulePlanItem amzAdvSchedulePlanItem = null;
		if (list != null && list.size() > 0) {
			for (int i = 0; i < list.size(); i++) {
				amzAdvSchedulePlanItem = list.get(i);
				String taskId = amzAdvSchedulePlanItem.getTaskid();
				sysTimeTaskService.deleteTimeTask(taskId);
				amzAdvSchedulePlanItemMapper.delete(amzAdvSchedulePlanItem);
			}
		}
		Example example2 = new Example(AmzAdvSchedulePlanData.class);
		Criteria crit2 = example2.createCriteria();
		crit2.andEqualTo("planid", planId);
		amzAdvSchedulePlanDataMapper.deleteByExample(example2);
		this.delete(planId);
	}
	
	public void deletePlanDateForInvalid(){
		List<Map<String, Object>> list = amzAdvSchedulePlanMapper.selectAllPlanDate();
		for(Map<String, Object> map : list) {
			BigInteger campaignid = (BigInteger) map.get("campaignId");
			BigInteger adgroupid = (BigInteger) map.get("adGroupId");
			BigInteger adid = (BigInteger) map.get("adId");
			BigInteger keywordid = (BigInteger) map.get("keywordId");
			BigInteger planid = (BigInteger) map.get("planid");
			if(keywordid != null && keywordid.compareTo(BigInteger.ZERO) != 0) {
				String keywordText = map.get("keywordText") == null ? null : map.get("keywordText").toString();
				if(keywordText != null) {
					continue;
				}else {
					deletePlan(planid.toString());
				}
			}
			if(adid != null && adid.compareTo(BigInteger.ZERO) != 0) {
				String productads_sku = map.get("productads_sku") == null ? null : map.get("productads_sku").toString();
				if(productads_sku != null) {
					continue;
				}else {
					deletePlan(planid.toString());
				}
			}
			if(adgroupid != null && adgroupid.compareTo(BigInteger.ZERO) != 0) {
				String adgroups_name = map.get("adgroups_name") == null ? null : map.get("adgroups_name").toString();
				if(adgroups_name != null) {
					continue;
				}else {
					deletePlan(planid.toString());
				}
			}
			if(campaignid != null && campaignid.compareTo(BigInteger.ZERO) != 0) {
				String campaigns_name = map.get("campaigns_name") == null ? null : map.get("campaigns_name").toString();
				if(campaigns_name != null) {
					continue;
				}else {
					deletePlan(planid.toString());
				}
			}
		}
	} 

}
