package com.example.demo.pojo.expand;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Date;
import java.sql.Time;

@Data
@AllArgsConstructor
public class UserMessage {
    private String tableName;
    private Integer messageId;
    private String messageContent;// 消息内容
    private Short messageType;
    private Integer messageCreator;// 消息的发送者
    private Date messageDate; // 消息发送日期
    private Time messageTime;// 发送时间
}
