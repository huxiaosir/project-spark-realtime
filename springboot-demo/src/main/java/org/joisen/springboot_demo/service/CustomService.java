package org.joisen.springboot_demo.service;

import org.springframework.stereotype.Service;

/**
 * @Author Joisen
 * @Date 2023/1/3 16:28
 * @Version 1.0
 */
public interface CustomService {
    String doLogin(String username, String password);
}
