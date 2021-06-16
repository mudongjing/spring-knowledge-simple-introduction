package org.example.controller.jsontest;

import com.google.gson.Gson;
import org.example.bean.User;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class jsonbodyController {
    @GetMapping("/jsonbody")
    public ResponseEntity<User> jsonBody(RequestEntity<User> request){
        String str=null;
        User user=request.getBody();
        ResponseEntity<User> responseEntity=new
                ResponseEntity<>(user,new HttpHeaders(), HttpStatus.OK);
        return responseEntity;
    }
}
