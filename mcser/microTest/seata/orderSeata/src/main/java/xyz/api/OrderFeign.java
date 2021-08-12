package xyz.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import xyz.api.pojo.Stock;

@FeignClient(name = "stockSeata-service",path = "/stock")
public interface OrderFeign {
    @RequestMapping("/dele/{id}")
    Stock dele(@PathVariable Integer id);

    @RequestMapping("/add")
    @ResponseBody
    Stock add();
}
