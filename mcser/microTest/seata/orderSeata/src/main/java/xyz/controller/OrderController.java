package xyz.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import xyz.api.pojo.Stock;
import xyz.pojo.Order;
import xyz.service.OrderService;
@Controller
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService orderService;
    private final RestTemplate restTemplate;


    @Autowired
    public OrderController(RestTemplate restTemplate) {this.restTemplate = restTemplate;}

    @RequestMapping("/add/{id}")
    @ResponseBody
    public Order add(@PathVariable Integer id){
        System.out.println("add"+id);
        Order order=orderService.addOrder(id);
//        String url=String.format("http://%s/stock/dele/2","stockSeata-service");
//        Stock stock= restTemplate.getForObject(url, Stock.class);
        return order;
    }
}
