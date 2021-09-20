package com.example.demo.pojo.expand;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Date;
import java.sql.Time;

@Data
@AllArgsConstructor
public class ExpandGroupMessage {
    private String tableName;
    private Integer messageId;
    private String messageContent;
    private Short messageType;
    private Integer messageCreator;
    private Date messageDate;
    private Time messageTime;
}
