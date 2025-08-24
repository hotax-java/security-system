package com.webapp.security.sso.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

//@Controller
//@RequestMapping
public class LoginTestController {
    @RequestMapping("/login")
    public String test() {
        return "login2";
    }
}
