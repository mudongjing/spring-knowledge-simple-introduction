package xyz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan(basePackages = "xyz.dao")
@EnableTransactionManagement
@EnableFeignClients
@EnableDiscoveryClient
public class orderSeataApplication {
    public static void main(String[] args) {
        SpringApplication.run(orderSeataApplication.class,args);
    }
}
