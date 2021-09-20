package com.example.demo.pojo;

import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.sql.Date;
import java.sql.Time;


@Data
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer messageId;
    private String messageContent;// 消息内容
    private Short messageType;/*
                                0：普通文本
                                1：大文本，上述的消息内容为文本文件地址
                                2：音频
                                3：视频
                                */
    private Integer messageCreator;// 消息的发送者
    private Date messageDate; // 消息发送日期
    private Time messageTime;// 发送时间
}
