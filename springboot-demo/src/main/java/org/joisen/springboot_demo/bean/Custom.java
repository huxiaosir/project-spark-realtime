package org.joisen.springboot_demo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @Author Joisen
 * @Date 2023/1/3 14:46
 * @Version 1.0
 */
@NoArgsConstructor
@Data
@ToString
@AllArgsConstructor
public class Custom {
    private String uname;
    private String password;
    private String address;
    private Integer age;
    private String gender;

    public static void main(String[] args) {
        Custom custom = new Custom();

    }
}
