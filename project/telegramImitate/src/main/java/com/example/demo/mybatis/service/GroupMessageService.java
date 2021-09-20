package com.example.demo.mybatis.service;

import java.sql.Date;
import java.sql.Time;

public interface GroupMessageService {
    int createMessageTable(String name);
    int insertMessageItem(String tableName,String content, Integer creator, Date date, Time time);
}
