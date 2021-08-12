package xyz.pojo.Handler;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import xyz.pojo.User;

public class BlockHandler {

    public static User blockHandlerForGetUser(String id, BlockException e){
        e.printStackTrace();
        return new User("流控！！");
    }
}
