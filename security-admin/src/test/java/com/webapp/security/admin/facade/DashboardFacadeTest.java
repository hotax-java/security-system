package com.webapp.security.admin.facade;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
class DashboardFacadeTest {
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Test
    void getDashboardInfo() {
        String encode = passwordEncoder.encode("webapp-secret");
        System.out.println("passwordEncoder: -> " + encode);
    }
}