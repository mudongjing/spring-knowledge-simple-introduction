package org.example.convertorwrap;

import com.google.gson.Gson;
import org.example.bean.User;
import org.springframework.format.Formatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserFormatter  implements Formatter<User> {
    @Override
    public String print(User user, Locale locale) {
        return (new Gson()).toJson(user);
    }

    @Override
    public User parse(String source, Locale locale) throws ParseException {
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

}
