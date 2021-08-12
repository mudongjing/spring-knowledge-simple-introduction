package xyz.controller.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;


@FeignClient(name="stock-service",path="/stock",fallback = StockFeignServiceFallback.class)
public interface StockFeignService {

    @RequestMapping("/back")
    String back();
}
