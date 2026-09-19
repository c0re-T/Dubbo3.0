package com.ittxf.consumer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final RestTemplate restTemplate;

    public String getOrder() {
        String rs = restTemplate.getForObject("http://localhost:8081/user", String.class);
        return "Order Service:" + rs;
    }
}
