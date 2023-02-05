package com.brice.filter;

import com.alibaba.fastjson.JSON;
import com.brice.common.BaseContext;
import com.brice.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录过滤器
 */
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {

    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        //1.获取本次请求的URI
        String requestURI = request.getRequestURI();

        //2.定义放行的内容
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                "/doc.html",
                "/webjars/**",
                "/swagger-resources",
                "/v2/api-docs",
                "/user/sendMsg", //移动端发送验证码
                "/user/login" //移动端登录
        };

        //3.判断是否是放行的内容
        if (check(urls, requestURI)){
            filterChain.doFilter(request, response);
            return;
        }

        //4-1.判断用户是否已经登录
        if (request.getSession().getAttribute("employee") != null){
            //将id存入线程
            Long empId = (Long) request.getSession().getAttribute("employee");
            BaseContext.setCurrentId(empId);
            //已经登录，放行
            filterChain.doFilter(request, response);
            return;
        }

        //4-2.判断移动端用户是否已经登录
        if (request.getSession().getAttribute("user") != null){
            //将id存入线程
            Long userId = (Long) request.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);
            //已经登录，放行
            filterChain.doFilter(request, response);
            return;
        }

        //5.没有登录，根据前端的拦截器格式返回数据
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
    }

    /**
     * 放行判断方法
     */
    public boolean check(String[] urls, String requestURI){
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match){
                return true;
            }
        }
        return false;
    }
}
