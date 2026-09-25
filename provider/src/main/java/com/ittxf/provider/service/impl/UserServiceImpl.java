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

    @Override
    public StreamObserver<String> sayHelloClientStream(StreamObserver<String> response) {
        return new StreamObserver<String>() {
            @Override
            public void onNext(String data) {
                // 服务端接受数据
                System.out.println("接受到结果：" + data);

                // 服务端处理数据；此处不能调用 response.onCompleted()，否则响应流提前结束
                response.onNext("响应结果:" + data);
                response.onNext("hello:" + data);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("服务端处理错误：" + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("服务端处理完成");
                // 请求流结束后再统一完成响应流
                response.onCompleted();
            }
        };
    }
}
