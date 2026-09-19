package com.ittxf.consumer.controller;

import com.ittxf.consumer.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/order")
    public String getOrder() {
        return orderService.getOrder();
    }
}
