package org.joisen.springboot_demo.service.impl;

import org.joisen.springboot_demo.bean.Custom;
import org.joisen.springboot_demo.mapper.CustomMapper;
import org.joisen.springboot_demo.service.CustomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author Joisen
 * @Date 2023/1/3 16:28
 * @Version 1.0
 */
@Service
// spring给该类默认的名字作为类名首字母为小写的形式 -> customServiceImpl
// 也可以指定 @Service(value="newName")
public class CustomServiceImpl implements CustomService {

    @Autowired
    CustomMapper customMapper;
    @Override
    public String doLogin(String username, String password) {
        System.out.println("CustomServiceImpl: 业务处理");
        // 数据校验


        // 调用数据层，对比数据库中的数据是否一致
        Custom custom = customMapper.searchByUsernameAndPassword(username, password);
        if(custom != null) return "ok";
        return "error";
    }
}
