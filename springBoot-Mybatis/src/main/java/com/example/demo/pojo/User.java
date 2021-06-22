package com.example.demo.pojo;

import lombok.*;
import org.springframework.data.annotation.TypeAlias;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TypeAlias("USER")
public class User {
    private Integer id;
    private Integer userId;
    private String userName;
}
