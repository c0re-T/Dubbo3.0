package com.ittxf.provider.service.impl;

import com.ittxf.common.UserService;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(version = "2.0")
public class UserServiceImpl2 implements UserService {
    public String getUser() {
        return "User Service version 2.0";
    }
}
