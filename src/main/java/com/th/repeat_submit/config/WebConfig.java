package com.th.repeat_submit.config;

import com.th.repeat_submit.filter.RepeatableRequestFilter;
import com.th.repeat_submit.interceptor.RepeatSubmitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @program: repeat_submit
 * @description:
 * @author: xiaokaixin
 * @create: 2022-05-29 21:50
 **/
@Configuration
public class WebConfig implements WebMvcConfigurer {


    @Autowired
    RepeatSubmitInterceptor repeatSubmitInterceptor;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(repeatSubmitInterceptor).addPathPatterns("/**");
    }

    @Bean
    FilterRegistrationBean<RepeatableRequestFilter> repeatableRequestFilterFilterRegistrationBean(){
        FilterRegistrationBean<RepeatableRequestFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new RepeatableRequestFilter());
        bean.addUrlPatterns("/*");
        return bean;
    }
}
