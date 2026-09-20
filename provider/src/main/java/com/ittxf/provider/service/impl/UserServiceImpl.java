package com.ittxf.provider.service.impl;

import com.ittxf.common.UserService;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(version = "1.0")
public class UserServiceImpl implements UserService {
    public String getUser() {
        // 业务处理
        return "User Service version 1.0";
    }

    // Server_Stream：服务端流，处理一个请求返回多个结果
    @Override
    public void sayHelloServerStream(String name, StreamObserver<String> response) {
        // 处理name
        response.onNext("hello:" + name);
        // 处理name
        response.onNext("hello:" + name);

        // 完成处理
        response.onCompleted();
    }
}
