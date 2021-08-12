package xyz.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class Order {
    private Integer id;
    private Integer orderNum;
    public Order(Integer orderNum){
        this.orderNum=orderNum;
    }
}
