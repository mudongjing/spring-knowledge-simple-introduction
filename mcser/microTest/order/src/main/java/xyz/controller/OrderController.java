package xyz.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.controller.feign.StockFeignService;

@RestController
@RequestMapping("/order")
public class OrderController {

    //private final RestTemplate restTemplate;

    private final StockFeignService stockFeignService;
//    @Autowired
//    public OrderController(RestTemplate restTemplate) {this.restTemplate = restTemplate;}

    @Autowired
    public OrderController(StockFeignService stockFeignService){
        this.stockFeignService=stockFeignService;
    }

    @Value("${stockService.name}")
    private String stockName;

//    @ResponseBody
//    @RequestMapping("/add")
//    public String add (){
//        System.out.println("下单");
//        String url=String.format("http://%s/stock/reduct",stockName);
//        String forObject = restTemplate.getForObject(url, String.class);
//        return "addOrder"+forObject;
//    }



    @RequestMapping("/regret")
    public String reduce(){
        System.out.println("后悔购买");
        String msg=stockFeignService.back();
        return "用户后悔"+msg;
    }
}
