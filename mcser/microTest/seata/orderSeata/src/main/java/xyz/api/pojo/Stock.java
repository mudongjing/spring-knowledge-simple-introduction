package xyz.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Stock {
    private Integer id;
    private Integer stockNum;
    public Stock(Integer stockNum){
        this.stockNum=stockNum;
    }
}
