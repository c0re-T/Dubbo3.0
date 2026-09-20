package com.ittxf.consumer.service;

import com.ittxf.common.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.common.stream.StreamObserver;
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
        // UNARY
        // return "Order Service:" + userService.getUser();
        userService.sayHelloServerStream("dubbo", new StreamObserver<String>() {
            @Override
            public void onNext(String data) {
                System.out.println("接受到结果：" + data);
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        });
        return "Order Service:" + userService.getUser();
    }
}
