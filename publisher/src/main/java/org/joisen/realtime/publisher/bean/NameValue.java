package org.joisen.realtime.publisher.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author Joisen
 * @Date 2023/1/4 13:21
 * @Version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NameValue {
    private String name;
    private Object value;
}
