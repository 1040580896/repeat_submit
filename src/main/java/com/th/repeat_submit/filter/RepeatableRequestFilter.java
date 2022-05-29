package com.th.repeat_submit.filter;

import com.th.repeat_submit.request.RepeatableReadRequestWrapper;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @program: repeat_submit
 * @description:
 * @author: xiaokaixin
 * @create: 2022-05-29 22:01
 **/
public class RepeatableRequestFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        if(StringUtils.startsWithIgnoreCase(request.getContentType(),"application/json")){
            RepeatableReadRequestWrapper requestWrapper = new RepeatableReadRequestWrapper(request, (HttpServletResponse) servletResponse);
            filterChain.doFilter(requestWrapper,servletResponse);
            return;
        }
        filterChain.doFilter(servletRequest,servletResponse);

    }
}
