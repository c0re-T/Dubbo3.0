package com.ittxf.consumer.service;

import com.ittxf.common.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
// @RequiredArgsConstructor
public class OrderService {

    // private final RestTemplate restTemplate;

    @DubboReference(version = "1.0")
    private UserService userService;

    public String getOrder() {
        return "Order Service:" + userService.getUser();
    }
}
