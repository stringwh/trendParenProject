package cn.how2j.trend.service;
 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
 
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
 
import cn.how2j.trend.pojo.IndexData;
import cn.how2j.trend.util.SpringContextUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
 
@Service
@CacheConfig(cacheNames="index_datas")
public class IndexDataService {
    private Map<String, List<IndexData>> indexDatas=new HashMap<>();//获取数据用map 会获取多条
    @Autowired RestTemplate restTemplate;
 
    @HystrixCommand(fallbackMethod = "third_part_not_connected")//断路器
    public List<IndexData> fresh(String code) {
        List<IndexData> indexeDatas =fetch_indexes_from_third_part(code);
         
        indexDatas.put(code, indexeDatas);
         
        System.out.println("code:"+code);
        System.out.println("indexeDatas:"+indexDatas.get(code).size());
         
        IndexDataService indexDataService = SpringContextUtil.getBean(IndexDataService.class);
        indexDataService.remove(code);
        return indexDataService.store(code);
    }
     
    @CacheEvict(key="'indexData-code-'+ #p0")//代码编号
    public void remove(String code){
         
    }
 
    @CachePut(key="'indexData-code-'+ #p0")
    public List<IndexData> store(String code){
        return indexDatas.get(code);
    }
 
    @Cacheable(key="'indexData-code-'+ #p0")
    public List<IndexData> get(String code){
        return CollUtil.toList();
    }
     //获取数据
    public List<IndexData> fetch_indexes_from_third_part(String code){
        List<Map> temp= restTemplate.getForObject("http://127.0.0.1:8090/indexes/"+code+".json",List.class);
        return map2IndexData(temp);
    }
     
    private List<IndexData> map2IndexData(List<Map> temp) {//map转换IndexData格式
        List<IndexData> indexDatas = new ArrayList<>();
        for (Map map : temp) {
            String date = map.get("date").toString();
            float closePoint = Convert.toFloat(map.get("closePoint"));
            IndexData indexData = new IndexData();
             
            indexData.setDate(date);
            indexData.setClosePoint(closePoint);
            indexDatas.add(indexData);
        }
         
        return indexDatas;
    }
 
    public List<IndexData> third_part_not_connected(String code){//第三方数据断开所调用的方法
        System.out.println("third_part_not_connected()");
        IndexData index= new IndexData();
        index.setClosePoint(0);
        index.setDate("n/a");
        return CollectionUtil.toList(index);
    }
         
}