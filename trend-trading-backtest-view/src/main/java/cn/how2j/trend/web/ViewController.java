package cn.how2j.trend.web;
 
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
  
@Controller
public class ViewController {
    @GetMapping("/")
    public String index() throws Exception {
        return "index";
    }
    @GetMapping("/view")
    public String view() throws Exception {
        return "view";
    }
    @GetMapping("/view2")
    public String view2() throws Exception {
        return "view2";
    }
}