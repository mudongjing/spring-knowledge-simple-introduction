package xyz.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import xyz.pojo.Stock;
import xyz.service.StockService;

@Controller
@RequestMapping("/stock")
public class StockController {
    @Autowired
    private StockService stockService;

    @RequestMapping("/dele/{id}")
    @ResponseBody
    public Stock dele(@PathVariable Integer id){
        Stock stock=stockService.deleStock(id);
        return stock;
    }
    @RequestMapping("/add")
    @ResponseBody
    public Stock add(){
        System.out.println("stock add");
        Stock stock=new Stock(5);
        stockService.insert(stock);
        return stock;
    }
}
