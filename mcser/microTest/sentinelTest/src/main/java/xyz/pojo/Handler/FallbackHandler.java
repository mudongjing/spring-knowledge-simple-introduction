package xyz.pojo.Handler;

import xyz.pojo.User;

public class FallbackHandler {

    public static User fallbackHandlerForGetUser(String id,Throwable e){
        e.printStackTrace();
        return new User("异常！！");
    }
}
