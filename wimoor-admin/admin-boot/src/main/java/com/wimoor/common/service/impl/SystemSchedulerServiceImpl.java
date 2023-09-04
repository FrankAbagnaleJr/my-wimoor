package com.wimoor.common.service.impl;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.mapper.QuartzTaskMapper;
import com.wimoor.common.pojo.entity.QuartzJobsVO;
import com.wimoor.common.pojo.entity.QuartzTask;
import com.wimoor.common.service.SystemSchedulerService;

@Service
public class SystemSchedulerServiceImpl implements SystemSchedulerService {
    @Autowired
    SchedulerFactoryBeanWithShutdownDelay schedulerFactory;
    @Autowired
    QuartzTaskMapper quartzTaskMapper;

    /**
     * 根据数据库表中的对象 把任务 添加到调度器
     *
     * @param quartzTask
     * @return 添加成功或者失败
     */
    @Override
    public boolean addScheduler(QuartzTask quartzTask) {
        try {
            // 得到调度器
            Scheduler scheduler = schedulerFactory.getScheduler();
            // 创建任务
            JobDetail jobDetail = buildJod(quartzTask);
            // 创建cron调度策略,这里配置了错过策略是执行错过的第一个任务，其他按照正常任务执行
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(quartzTask.getCron()).withMisfireHandlingInstructionFireAndProceed();
            // 构建触发器trigger
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(quartzTask.getName(), quartzTask.getFgroup())//设置一个触发器标识,这里设置了跟JobDetail使用一样的命名以及分组
                    .forJob(jobDetail)//绑定trigger到jobdetail
                    .withSchedule(scheduleBuilder)
                    .withPriority(quartzTask.getPriority())
                    .build();
            // 准备调度
            scheduler.scheduleJob(jobDetail, trigger);
            return true;
        } catch (SchedulerException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * 根据传入的任务列表创建任务对象返回
     *
     * @param quartzTask
     * @return 返回任务对象
     */
    private static JobDetail buildJod(QuartzTask quartzTask) {
        JobKey jobKey = JobKey.jobKey(quartzTask.getName(), quartzTask.getFgroup());
        //开始封装jobDataMap
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("id", quartzTask.getId());
        jobDataMap.put("jobPath", quartzTask.getPath());
        jobDataMap.put("parameter", quartzTask.getParameter());
        jobDataMap.put("description", quartzTask.getDescription());
        jobDataMap.put("bean", quartzTask.getBean());
        jobDataMap.put("method", quartzTask.getMethod());
        jobDataMap.put("createTime", System.currentTimeMillis());
        //返回任务对象
        return JobBuilder.newJob(QuartzTaskFactory.class)
                .withIdentity(jobKey)
                .withDescription(quartzTask.getDescription())
                .storeDurably()
                .requestRecovery(false)
                .setJobData(jobDataMap)
                .build();
    }

    /**
     * 刷新任务，先删除全部任务，再重新插入任务
     */
    public void refreshTask() {
        deleteAllTask();
        insertTask();
    }

    /**
     * 删除全部任务
     */
    public void deleteAllTask() {
        List<QuartzJobsVO> slist = this.listScheduler();
        for (QuartzJobsVO item : slist) {
            this.deleteScheduler(item.getJobDetailName(), item.getGroupName());
        }
    }

    /**
     * 插入全部任务
     */
    public void insertTask() {
        QueryWrapper<QuartzTask> queryWrapper = new QueryWrapper<QuartzTask>();
        queryWrapper.eq("isdelete", 0);
        //把没逻辑删除的任务都查询出来
        List<QuartzTask> list = quartzTaskMapper.selectList(queryWrapper);
        //循环每一个任务，如果调度器里没有这个任务，那么就把任务添加到调度器里
        list.forEach(item -> {
            if (this.findScheduler(item.getName(), item.getFgroup()) == false) {
                this.addScheduler(item);
            }
        });
    }

    /**
     * 更新调度器
     *
     * @param jobDetailName
     * @param jobDetailGroup
     * @param cron
     * @return
     */
    @Override
    public boolean updateScheduler(String jobDetailName, String jobDetailGroup, String cron) {
        try {
            // 得到一个调度器
            Scheduler scheduler = schedulerFactory.getScheduler();
            // 创建任务key
            JobKey jobKey = JobKey.jobKey(jobDetailName, jobDetailGroup);
            //如果传入的cron是无效的 或 查不到这个任务，那么就返回false
            if (!CronExpression.isValidExpression(cron) || !scheduler.checkExists(jobKey)) {
                return false;
            }
            //triggerKey为添加定时任务时配置的name,group，这里是添加的时候设置的name跟group跟jobdetail是一样的
            TriggerKey triggerKey = TriggerKey.triggerKey(jobDetailName, jobDetailGroup);
            Trigger newTrigger = TriggerBuilder
                    .newTrigger()
                    .withIdentity(triggerKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron)).build();
            //把新的任务和触发器添加到调度器中
            scheduler.rescheduleJob(triggerKey, newTrigger);
            return true;
        } catch (SchedulerException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除调度器，根据任务名字和组来删除
     * @param jobDetailName 任务名字
     * @param jobDetailGroup 任务组
     * @return
     */
    @Override
    public boolean deleteScheduler(String jobDetailName, String jobDetailGroup) {
        try {
            //得到调度器
            Scheduler scheduler = schedulerFactory.getScheduler();
            //根据任务名字和组创建任务key
            JobKey jobKey = JobKey.jobKey(jobDetailName, jobDetailGroup);
            //去调度器中查询人任务key，如果不存在返回false
            if (!scheduler.checkExists(jobKey)) {
                return false;
            }
            //如果存在那么就删除
            scheduler.deleteJob(jobKey);
            return true;
        } catch (SchedulerException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * 暂停调度器
     * @param jobDetailName
     * @param jobDetailGroup
     * @return
     */
    @Override
    public boolean puaseScheduler(String jobDetailName, String jobDetailGroup) {
        try {
            Scheduler scheduler = schedulerFactory.getScheduler();
            JobKey jobKey = JobKey.jobKey(jobDetailName, jobDetailGroup);
            if (!scheduler.checkExists(jobKey)) {
                return false;
            }
            scheduler.pauseJob(jobKey);
            return true;
        } catch (SchedulerException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 恢复调度器
     * @param jobDetailName
     * @param jobDetailGroup
     * @return
     */
    @Override
    public boolean resumeScheduler(String jobDetailName, String jobDetailGroup) {
        try {
            Scheduler scheduler = schedulerFactory.getScheduler();
            JobKey jobKey = JobKey.jobKey(jobDetailName, jobDetailGroup);
            if (!scheduler.checkExists(jobKey)) {
                return false;
            }
            scheduler.resumeJob(jobKey);
            return true;
        } catch (SchedulerException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 查找调度器
     * @param jobDetailName
     * @param jobDetailGroup
     * @return
     */
    @Override
    public boolean findScheduler(String jobDetailName, String jobDetailGroup) {
        try {
            Scheduler scheduler = schedulerFactory.getScheduler();
            JobKey jobKey = JobKey.jobKey(jobDetailName, jobDetailGroup);
            if (!scheduler.checkExists(jobKey)) {
                return false;
            }
            return true;
        } catch (SchedulerException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * 得到所有的任务对象
     * @return
     */
    public List<QuartzJobsVO> listScheduler() {
        // TODO Auto-generated method stub
        //得到调度器
        Scheduler scheduler = schedulerFactory.getScheduler();
        //创建组匹配器
        GroupMatcher<JobKey> matcher = GroupMatcher.anyJobGroup();
        //创建孔的任务key集合
        Set<JobKey> jobKeys = null;
        //创建一个任务值对象集合
           //QuartzJobsVO里面有任务名字、任务组、cron表达式、时区、状态
        List<QuartzJobsVO> jobList = new ArrayList<QuartzJobsVO>();
        try {
            //从调度去中根据组匹配器获取任务key的集合
            jobKeys = scheduler.getJobKeys(matcher);
            //循环集合每一个任务key
            for (JobKey jobKey : jobKeys) {
                //从调度器中根据任务key取所有的触发器
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                //循环每一个触发器
                for (Trigger trigger : triggers) {
                    //新建一个任务对象，开始赋值封装，封装完了添加到jobList中
                    QuartzJobsVO job = new QuartzJobsVO();
                    job.setJobDetailName(jobKey.getName());
                    job.setGroupName(jobKey.getGroup());
                    job.setJobCronExpression("触发器:" + trigger.getKey());
                    Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
                    job.setStatus(triggerState.name());
                    //如果是cron的调度器，那么重新封装cron表达式和时区
                    if (trigger instanceof CronTrigger) {
                        CronTrigger cronTrigger = (CronTrigger) trigger;
                        String cronExpression = cronTrigger.getCronExpression();
                        job.setJobCronExpression(cronExpression);
                        job.setTimeZone(cronTrigger.getTimeZone().getID());
                    }
                    jobList.add(job);
                }
            }

        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        return jobList;

    }

}
