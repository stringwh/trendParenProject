package cn.how2j.trend.controller;
 
import java.util.List;
 
import cn.how2j.trend.config.IpConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
 
import cn.how2j.trend.pojo.Index;
import cn.how2j.trend.service.IndexService;
  /*
  *
  *
  * */
@RestController
public class IndexController {
    @Autowired IndexService indexService;
    @Autowired IpConfiguration ipConfiguration;
     
//  http://127.0.0.1:8011/codes
     //返回代码集合，并通过 IpConfiguration 获取当前接口并打印。
    @GetMapping("/codes")
    @CrossOrigin//@CrossOrigin 表示允许跨域，因为后续的回测视图是另一个端口号的，访问这个服务是属于跨域了。
    public List<Index> codes() throws Exception {
        System.out.println("current instance's port is "+ ipConfiguration.getPort());
        return indexService.get();
    }
}