package xyz.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stock")
public class StockController {

    @RequestMapping("/reduce")
    public String reduce(){
        System.out.println("库存减一");
        return "仓库拣货";
    }

    @RequestMapping("/back")
    public String back(){
        System.out.println("货物返回");
        return "订单撤销";
    }

}
