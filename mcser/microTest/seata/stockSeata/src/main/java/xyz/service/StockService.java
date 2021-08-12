package xyz.service;

import xyz.pojo.Stock;

public interface StockService {
    Stock deleStock(Integer id);
    void insert(Stock stock);
}
