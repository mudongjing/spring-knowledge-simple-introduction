package com.example.demo.mybatis.service.impl;

import com.example.demo.mybatis.mapper.GroupMessageMapper;
import com.example.demo.mybatis.service.GroupMessageService;
import com.example.demo.pojo.expand.ExpandGroupMessage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Date;
import java.sql.Time;
import java.util.Random;

@Service
@Slf4j
public class GroupMessageServiceImpl implements GroupMessageService {
    @Resource
    private GroupMessageMapper groupMessageMapper;
    @Override
    public int createMessageTable(String name) {
        return groupMessageMapper.createGroupMessageTable(name);
    }

    @Override
    public int insertMessageItem(String tableName,String content,
                                 Integer creator, Date date, Time time) {
        Short type=new Short("0");
        ExpandGroupMessage expandGroupMessage=null;
        if(content.length()>255){// 需要存储为文件，文件名使用发布者id+发布时间+随机值
            type=1;
            content=createFileForContent(creator,date,time,content);
            expandGroupMessage=new ExpandGroupMessage(
                    tableName,null,content,type,creator,date,time);
        }else{// 普通文本
            expandGroupMessage=new ExpandGroupMessage(
                    tableName,null,content,type,creator,date,time);
        }
        System.out.println("messageTable: "+expandGroupMessage.getTableName());
        System.out.println("messageDate: "+expandGroupMessage.getMessageDate());
        System.out.println("messageTime: "+expandGroupMessage.getMessageTime());
        return groupMessageMapper.insertMessageItem(expandGroupMessage);
    }


    @SneakyThrows
    private String createFileForContent(Integer creator,Date date,Time time,String content){
        String way="E:\\FFOutput\\test\\";
        String fileTemp=creator+"_"+date+"_"+time+"_"+new Random().nextInt(9999)+".txt".replace(':','+');
        String newFile=way+fileTemp;
        File file=new File(newFile);
        file.createNewFile();
        BufferedWriter out=new BufferedWriter(new FileWriter(file));
        out.write(content);
        out.close();
        return newFile;
    }
}
