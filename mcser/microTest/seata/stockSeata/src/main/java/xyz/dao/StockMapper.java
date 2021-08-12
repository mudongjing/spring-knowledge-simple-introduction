package xyz.dao;

import org.apache.ibatis.annotations.Mapper;
import xyz.pojo.Stock;

@Mapper
public interface StockMapper {
    void addStock(Stock stock);
    void deleStock(Integer id);
    Stock gainStock(Integer id);
}
