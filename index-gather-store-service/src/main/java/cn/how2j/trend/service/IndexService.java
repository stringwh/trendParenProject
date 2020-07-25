package cn.how2j.trend.service;

import cn.how2j.trend.pojo.Index;
import cn.how2j.trend.util.SpringContextUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
//指数代码
@Service
@CacheConfig(cacheNames="indexes")
public class IndexService {
    private List<Index> indexes;
    @Autowired RestTemplate restTemplate;
/*
如果一开始忘记启动第三方了，那么redis里保存的就会是断路信息
如果第三方刷新了， redis 也没有办法刷新
所以我们现在来解决这个问题
刷新数据。 刷新的思路就是：
 先运行 fetch_indexes_from_third_part 来获取数据
 删除数据
保存数据
从而达到刷新的效果
//SpringContextUtil.getBean 去重新获取了一次 IndexService，
为什么不在刷新中remove, store 方法， 而要重新获取一次呢？ 这个是因为 springboot 的机制大概有这么个 bug
 从已经存在的方法里调用 redis 相关方法，并不能触发 redis 相关操作，所以只好用这种方式重新获取一次了。
*/
    @HystrixCommand(fallbackMethod = "third_part_not_connected")
    public List<Index> fresh() {
        indexes =fetch_indexes_from_third_part();
        //SpringContextUtil.getBean 去重新获取了一次 IndexService，
        IndexService indexService = SpringContextUtil.getBean(IndexService.class);
        indexService.remove();
        return indexService.store();
    }

    @CacheEvict(allEntries=true)//清空数据
    public void remove(){

    }

    @Cacheable(key="'all_codes'")//保存数据，这个专门用来往 redis 里保存数据，注意： 这个 indexes 是一个成员变量。
    public List<Index> store(){
        System.out.println(this);
        return indexes;
    }

    @Cacheable(key="'all_codes'")// 获取数据，这个就是专门用来从 redis 中获取数据
    public List<Index> get(){
        return CollUtil.toList();
    }

    public List<Index> fetch_indexes_from_third_part(){
        List<Map> temp= restTemplate.getForObject("http://127.0.0.1:8090/indexes/codes.json",List.class);
        return map2Index(temp);
    }
    private List<Index> map2Index(List<Map> temp) {
        List<Index> indexes = new ArrayList<>();
        for (Map map : temp) {
            String code = map.get("code").toString();
            String name = map.get("name").toString();
            Index index= new Index();
            index.setCode(code);
            index.setName(name);
            indexes.add(index);
        }

        return indexes;
    }

    public List<Index> third_part_not_connected(){
        System.out.println("third_part_not_connected()");
        Index index= new Index();
        index.setCode("000000");
        index.setName("无效指数代码");
        return CollectionUtil.toList(index);
    }

}