package xyz.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.controller.feign.StockFeignService;

@RestController
@RequestMapping("/senfei")
public class SentinelFeiController {
    private final StockFeignService stockFeignService;
    @Autowired
    public SentinelFeiController(StockFeignService stockFeignService){
        this.stockFeignService=stockFeignService;
    }
    @RequestMapping("/add")
    public String add(){
        String msg=stockFeignService.back();
        return "用户后悔"+msg;
    }

}
