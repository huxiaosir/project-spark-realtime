package org.joisen.realtime.publisher.service;

import org.joisen.realtime.publisher.bean.NameValue;

import java.util.List;
import java.util.Map;

/**
 * @Author Joisen
 * @Date 2023/1/4 10:04
 * @Version 1.0
 */
public interface PublisherService {
    Map<String, Object> doDauRealtime(String td);

    List<NameValue> doStatsByItem(String itemName, String date, String t);

    Map<String, Object> doDetailByItem(String date, String itemName, Integer pageNo, Integer pageSize);
}
