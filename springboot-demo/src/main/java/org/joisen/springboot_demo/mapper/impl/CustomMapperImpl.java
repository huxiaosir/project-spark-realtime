package org.joisen.springboot_demo.mapper.impl;

import org.joisen.springboot_demo.bean.Custom;
import org.joisen.springboot_demo.mapper.CustomMapper;
import org.springframework.stereotype.Repository;

/**
 * @Author Joisen
 * @Date 2023/1/3 16:52
 * @Version 1.0
 */
@Repository
public class CustomMapperImpl implements CustomMapper {

    @Override
    public Custom searchByUsernameAndPassword(String username, String password) {
        // 查询数据库
        System.out.println("CustomMapperImpl : 查询数据库");
        return new Custom(username, password,null, null, null);
//        return null;
    }
}
