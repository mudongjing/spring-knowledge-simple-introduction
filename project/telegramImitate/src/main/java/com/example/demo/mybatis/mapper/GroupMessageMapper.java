package com.example.demo.mybatis.mapper;

import com.example.demo.pojo.expand.ExpandGroupMessage;

public interface GroupMessageMapper {
    int createGroupMessageTable(String name);
    int insertMessageItem(ExpandGroupMessage expandGroupMessage);
}
