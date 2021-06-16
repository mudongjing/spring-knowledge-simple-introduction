package org.example.controller.json;

import org.example.bean.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

@Controller
public class jsonController {
    @GetMapping("/json")
    @ResponseBody
    public Map<String,Object> callable() throws ParseException {
        Map<String,Object> map=new HashMap<>();
        User user=new User("wcd","25");
        map.put("user",user);
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd");
        Date date=format.parse("2021-12-26");
        map.put("date",date);
        return map;
    }
}
