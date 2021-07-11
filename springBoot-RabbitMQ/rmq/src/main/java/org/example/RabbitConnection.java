package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.concurrent.TimeoutException;

public class RabbitConnection {
    //工厂是可以常驻的
    private static ConnectionFactory connectionFactory=new ConnectionFactory();
    public static Connection createConnect(){
        try {
            ResourceBundle resource = ResourceBundle.getBundle("rabbitmq");//读取我们的属性文件
            connectionFactory.setHost(resource.getString("rabbitmq.localhost"));
            connectionFactory.setPort(Integer.parseInt(resource.getString("rabbitmq.port")));
            connectionFactory.setVirtualHost(resource.getString("rabbitmq.vhost"));
            connectionFactory.setUsername(resource.getString("rabbitmq.username"));
            connectionFactory.setPassword(resource.getString("rabbitmq.password"));
            return connectionFactory.newConnection();
        }catch (Exception e){ e.printStackTrace(); }
        return null;
    }
    public static void closeChnnelAndconnection(Channel channel,Connection connection){
            try {
                if(channel!=null) channel.close();
                if(connection!=null) connection.close();
            } catch (IOException e) { e.printStackTrace(); }
              catch (TimeoutException e) { e.printStackTrace();}
    }



}
