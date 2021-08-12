package xyz.controller.handler;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import xyz.controller.handler.domain.ResultSimple;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/*如果自己项目中对于这些各种异常的反应没有太多的不同
比如，有的的api如果出现异常，你可能本来希望让它返回一些特殊的信息，
此时，就表示不同的接口需要不同的一个异常处理机制。
但是，如果我们没有什么花里胡哨的操作，异常就是异常了，
只是返回一下该异常的信息，让用户注意一下，
此时，就可以声明这个类，其它的那些api上的对应的注解可以注释掉，所有的异常都会经由这个类来处理返回的信息
*/

@Slf4j
@Component
public class GlobalBlock implements BlockExceptionHandler {
    @Override
    public void handle(HttpServletRequest httpServletRequest,
                       HttpServletResponse httpServletResponse,
                       BlockException e) throws Exception {
        log.info("something =========="+e.getRule());
        ResultSimple r=null;
        if(e instanceof FlowException){
            r=ResultSimple.err(100,"接口限流");
        }
        httpServletResponse.setStatus(500);
        httpServletResponse.setCharacterEncoding("utf-8");
        httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        new ObjectMapper().writeValue(httpServletResponse.getWriter(),r);
    }
}
