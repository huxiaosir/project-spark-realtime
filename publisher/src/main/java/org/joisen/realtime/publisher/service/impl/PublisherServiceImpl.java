package org.joisen.realtime.publisher.service.impl;

import org.joisen.realtime.publisher.bean.NameValue;
import org.joisen.realtime.publisher.mapper.PublisherMapper;
import org.joisen.realtime.publisher.service.PublisherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @Author Joisen
 * @Date 2023/1/4 10:04
 * @Version 1.0
 */
@Service
public class PublisherServiceImpl implements PublisherService {

    @Autowired
    PublisherMapper publisherMapper;

    /**
     * 交易分析  -  明细查询
     * @param date
     * @param itemName
     * @param pageNo
     * @param pageSize
     * @return
     */
    @Override
    public Map<String, Object> doDetailByItem(String date, String itemName, Integer pageNo, Integer pageSize) {
        // 计算分页开始位置
        int from = (pageNo - 1) * pageSize;

        Map<String, Object> searchResult = publisherMapper.searchDetailByItem(date, itemName, from, pageSize);
        return searchResult;
    }


    /**
     * 日活分析业务处理
     * @param td
     * @return
     */
    @Override
    public Map<String, Object> doDauRealtime(String td) {
        // 调用mapper层方法查询ES数据库中的数据
        Map<String, Object> dauResult = publisherMapper.searchDau(td);
        return dauResult;
    }

    /**
     * 交易分析业务处理
     * @param itemName
     * @param date
     * @param t age => user_age  gender => user_gender
     * @return
     */
    @Override
    public List<NameValue> doStatsByItem(String itemName, String date, String t) {
        List<NameValue> searchResult = publisherMapper.searchStatsByItem(itemName, date, typeToField(t));
        return transformResult(searchResult, t);
    }



    public List<NameValue> transformResult(List<NameValue> searchResult, String t){
        if ("gender".equals(t)){
            if (searchResult.size()>0) {
                for (NameValue nameValue : searchResult) {
                    String name = nameValue.getName();
                    if(name.equals("F")){
                        nameValue.setName("女");
                    } else if (name.equals("M")) {
                        nameValue.setName("男");
                    }
                }
            }
            return searchResult;
        } else if ("age".equals(t)) {
            double totalAmount20 = 0;
            double totalAmount20To29 = 0;
            double totalAmount30 = 0;
            if(searchResult.size() > 0){
                for (NameValue nameValue : searchResult) {
                    Integer age = Integer.parseInt(nameValue.getName());
                    Double value = Double.parseDouble(nameValue.getValue().toString());
                    if (age < 20) {
                        totalAmount20 += value;
                    } else if (age < 30) {
                        totalAmount20To29 += value;
                    }else {
                        totalAmount30 += value;
                    }
                }
                searchResult.clear();
                searchResult.add(new NameValue("20岁以下", totalAmount20));
                searchResult.add(new NameValue("20到29岁", totalAmount20To29));
                searchResult.add(new NameValue("30岁以上", totalAmount30));

            }
            return searchResult;
        }else {
            return null;
        }
    }

    public String typeToField(String t){
        if("age".equals(t)){
            return "user_age";
        } else if ("gender".equals(t)) {
            return "user_gender";
        }else{
            return null;
        }
    }

}
