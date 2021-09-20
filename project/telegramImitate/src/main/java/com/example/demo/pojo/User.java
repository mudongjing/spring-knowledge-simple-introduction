package com.example.demo.pojo;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * telegram 在mysql中的用户模型
 *
 * 用户好友量表
 * 加入的群组。频道列表
 */
@Data
@Table(name="user")
public class User implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;
    private String userName;
    private String userFriends;
    private Short friendsType;// 0：普通文本，1：本地文件地址存储好友列表
    private String userGroups;
    private Short groupType;//0：是字符串表示的列表，1：存储为txt文件表示为集合

}
