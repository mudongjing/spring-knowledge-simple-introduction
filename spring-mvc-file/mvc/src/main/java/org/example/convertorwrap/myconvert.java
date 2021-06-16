package org.example.convertorwrap;

import com.google.gson.Gson;
import org.example.bean.User;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class myconvert implements Converter<String, User> {

    @Override
    public User convert(String source) {//user 1 ,2021-05-01
        User user=new User();
        String[] result=source.split(",");
        Date date=null;
        try{
            date=(new SimpleDateFormat("yyyy-MM-dd")).parse(result[1]);
        }catch(ParseException e){}
        user.setName(result[0]);
        user.setDate(date);
        return user;
    }
    public static void main(String[] args) throws ParseException {
        DefaultConversionService conversionService=new DefaultConversionService();
        conversionService.addConverter(new myconvert());
        User user=conversionService.convert("user 1 ,2021-05-01",User.class);
        Gson gson=new Gson();
        System.out.println(user);
        String user_string=gson.toJson(user);
        System.out.println(user_string);
        User user1=gson.fromJson(user_string,User.class);
        System.out.println(user1);
    }
}
