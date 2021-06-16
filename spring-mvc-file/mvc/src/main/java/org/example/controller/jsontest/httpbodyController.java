package org.example.controller.jsontest;

import com.google.gson.Gson;
import org.example.bean.User;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Controller
public class httpbodyController {
    @ResponseBody
    @GetMapping("/body")
    public User body(){
        String str=null;
        String url="http://localhost:8080/m/jsonbody";
        RestTemplate restTemplate=new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        User user=new User("sdsdd00","44545");
        String body=(new Gson()).toJson(user);
        HttpEntity<String> entity = new HttpEntity<String>(body, headers);
        ResponseEntity<User> user1=restTemplate.exchange(url, HttpMethod.GET, entity, User.class);
        User user2=user1.getBody();
        return user;
    }
}
