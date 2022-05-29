package com.th.repeat_submit.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: repeat_submit
 * @description:
 * @author: xiaokaixin
 * @create: 2022-05-29 21:48
 **/
@RestController
public class HelloController {

    @PostMapping("/hello")
    public String hello(@RequestBody String json){

        return json;
    }
}
