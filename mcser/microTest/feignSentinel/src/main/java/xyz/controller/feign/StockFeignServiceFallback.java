package xyz.controller.feign;

import org.springframework.stereotype.Component;

@Component
public class StockFeignServiceFallback implements StockFeignService{


    @Override
    public String back() {
        return "降级！";
    }
    
}
