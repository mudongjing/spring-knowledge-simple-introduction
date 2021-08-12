package xyz.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.dao.StockMapper;
import xyz.pojo.Stock;
import xyz.service.StockService;

@Service
public class StockServiceImpl implements StockService {
    private StockMapper stockMapper;
    @Autowired
    public StockServiceImpl(StockMapper stockMapper){
        this.stockMapper=stockMapper;
    }

    @Override
    public Stock deleStock(Integer id) {
        stockMapper.deleStock(id);
        return stockMapper.gainStock(id);
    }

    @Override
    public void insert(Stock stock) {
        stockMapper.addStock(stock);
    }
}
