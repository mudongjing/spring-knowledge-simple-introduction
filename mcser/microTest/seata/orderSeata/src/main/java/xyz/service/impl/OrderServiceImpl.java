package xyz.service.impl;

import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.api.OrderFeign;
import xyz.dao.OrderMapper;
import xyz.pojo.Order;
import xyz.service.OrderService;

@Service
public class OrderServiceImpl  implements OrderService {
    private OrderMapper orderMapper;
    @Autowired
    private OrderFeign orderFeign;
    @Autowired
    public OrderServiceImpl(OrderMapper orderMapper){
        this.orderMapper=orderMapper;
    }




//    @Override
//    public void addOrder(Order order) {
//        orderMapper.addOrder(order);
//    }

    @GlobalTransactional
    @Override
    public Order addOrder(Integer id) {
        orderMapper.addOrder(id);
        Order order=orderMapper.gainOrder(id);
        orderFeign.dele(id);
        return order;
    }
}
