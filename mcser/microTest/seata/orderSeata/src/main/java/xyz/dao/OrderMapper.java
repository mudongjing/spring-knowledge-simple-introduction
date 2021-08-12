package xyz.dao;

import org.apache.ibatis.annotations.Mapper;
import xyz.pojo.Order;

@Mapper
public interface OrderMapper {
    void addOrder(Integer id);
    void deleOrder(Integer id);
    Order gainOrder(Integer id);
}
