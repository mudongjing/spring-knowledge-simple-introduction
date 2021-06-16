package org.example.interceptor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SessionCheckInterceptor  implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) throws Exception {
        if(request.getSession().getAttribute("username")==null){
            response.sendRedirect("/login.jsp");
        }
        return true;//范围true后，请求将转移到postHandle，入宫返回false，则表示当前的方法不通过，无法继续下面的方法而结束
    }
    //上面完成了真正实现方法前的准备工作
    //下述方法，将真正执行对应的方法，并可能对request或response进行处理
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }
    //最后，在请求结束后，可能需要完成资源释放的工作
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
