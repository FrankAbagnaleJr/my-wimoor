QuartzTaskFactory类 继承了 QuartzJobBean 任务对象

    创建任务对象用的.class文件就是这个类

SystemSchedulerInit类 实现了 ApplicationRunner 接口

    在程序启动的时候会执行run方法，用来刷新所有quartz任务，开始拉去数据

SystemSchedulerServiceImpl 实现了 SystemSchedulerService 接口

    里面写了很多关于执行拉去亚马逊数据的方法
    1. addScheduler(QuartzTask quartzTask) 根据数据库表中的定时任务对象 把任务 添加到调度器
    2. buildJod(QuartzTask quartzTask) 把数据库中的数据封装成 JobDetail任务对象
    3. refreshTask()  刷新任务（先全删，再从数据库中查询逐个添加）
    4. deleteAllTask() 删除全部任务
    5. insertTask() 插入全部任务
    6. updateScheduler(String jobDetailName, String jobDetailGroup, String cron) 根据任务名字、组来更新调度器
    7. deleteScheduler(String jobDetailName, String jobDetailGroup) 根据任务名字和组来删除调度器
    8. puaseScheduler(String jobDetailName, String jobDetailGroup) 暂停调度器
    9. resumeScheduler(String jobDetailName, String jobDetailGroup) 恢复调度器
    10. findScheduler(String jobDetailName, String jobDetailGroup) 寻找任务
    11. listScheduler() 得到所有的List<QuartzJobsVO>任务对象，用来删除全部的任务

SchedulerFactoryBeanWithShutdownDelay 继承了 SchedulerFactoryBean 类

    调用了 父类的.destroy()方法，然后线程睡眠1秒钟。
    destroy()方法百度查了是 关闭bean factory shutdown上的Quartz调度程序，停止所有计划的作业