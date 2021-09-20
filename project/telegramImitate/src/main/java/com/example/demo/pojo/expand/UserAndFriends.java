package com.example.demo.pojo.expand;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserAndFriends {
    private Integer userId;
    private Integer friendId;
}
