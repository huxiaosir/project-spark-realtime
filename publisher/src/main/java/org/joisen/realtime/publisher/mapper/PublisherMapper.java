package org.joisen.realtime.publisher.mapper;

import org.joisen.realtime.publisher.bean.NameValue;

import java.util.List;
import java.util.Map;

/**
 * @Author Joisen
 * @Date 2023/1/4 10:05
 * @Version 1.0
 */
public interface PublisherMapper {
    Map<String, Object> searchDau(String td);

    /**
     * 交易分析 - 从ES中根据条件查询数据
     * @param itemName
     * @param date
     * @param field
     * @return
     */
    List<NameValue> searchStatsByItem(String itemName, String date, String field);

    Map<String, Object> searchDetailByItem(String date, String itemName, Integer from, Integer pageSize);
}
