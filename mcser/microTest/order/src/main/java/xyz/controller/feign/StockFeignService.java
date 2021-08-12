package xyz.controller.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

//name就是我们需要调用的那个服务名
//path是这个服务自己controller类上指定的映射，有的类可能没在类上指明，就不需要写

@FeignClient(name="stock-service",path="/stock")
public interface StockFeignService {
    //后面的步骤，基本就是把对应服务的controller类转化成对应的一个接口
    //读者会感觉，怎么好像抄了一遍
    //就像mybatis那样自己声明一下对应的接口，里面的内容，自然有其它的内容负责实现

    @RequestMapping("/reduce")
    String reduce();

    @RequestMapping("/back")
    String back();
}
