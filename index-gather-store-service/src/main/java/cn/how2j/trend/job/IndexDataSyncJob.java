package cn.how2j.trend.job;
 /*
 * 定时器任务类，同时刷新指数代码和指数数据。
 * */
import java.util.List;
 
import cn.hutool.core.date.DateUtil;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
 
import cn.how2j.trend.pojo.Index;
import cn.how2j.trend.service.IndexDataService;
import cn.how2j.trend.service.IndexService;
 
public class IndexDataSyncJob extends QuartzJobBean {
     
    @Autowired
    private IndexService indexService;
 
    @Autowired
    private IndexDataService indexDataService;
     
    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        System.out.println("定时启动：" + DateUtil.now());
        List<Index> indexes = indexService.fresh();//刷新指数代码
        for (Index index : indexes) {//每次从指数代码哪里获得code
             indexDataService.fresh(index.getCode());//刷新指数代码的数据存入数据库
        }
        System.out.println("定时结束：" + DateUtil.now());
 
    }
 
}