package org.joisen.springboot_demo.mapper;

/**
 * @Author Joisen
 * @Date 2023/1/3 16:51
 * @Version 1.0
 */

import org.joisen.springboot_demo.bean.Custom;

/**
 * MyBatis只写接口+SQL即可，不需要写实现类
 */
public interface CustomMapper {
    Custom searchByUsernameAndPassword(String username, String password);
}
