package com.example.demo.mybatis.mapper;

import com.example.demo.pojo.Group;

public interface GroupMapper {
    int insertGroupItem(Group group);
    int insertGroupMembers(Integer member);
}
