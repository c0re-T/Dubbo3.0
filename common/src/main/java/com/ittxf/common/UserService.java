package com.ittxf.common;

import org.apache.dubbo.common.stream.StreamObserver;


public interface UserService {
    // UNARY
    String getUser();

    // SERVER_STREAM
    default void sayHelloServerStream(String name, StreamObserver<String> response) {

    }

    // CLIENT_STREAM / BI_STREAM
    default StreamObserver<String> sayHelloClientStream(StreamObserver<String> response) {
        return response;
    }
}
