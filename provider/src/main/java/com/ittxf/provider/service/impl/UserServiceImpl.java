package com.ittxf.provider.service.impl;

import com.ittxf.common.UserService;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(version = "1.0")
public class UserServiceImpl implements UserService {
    public String getUser() {
        return "User Service version 1.0";
    }
}
