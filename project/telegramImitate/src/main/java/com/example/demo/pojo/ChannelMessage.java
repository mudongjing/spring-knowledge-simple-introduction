package com.example.demo.pojo;

import lombok.Data;

import java.sql.Date;
import java.sql.Time;

@Data
public class ChannelMessage {
    private Integer messageId;
    private String channelMessage;// 消息内容
    private Date messageDate; // 消息发送日期
    private Time messageTime;// 发送时间
}
