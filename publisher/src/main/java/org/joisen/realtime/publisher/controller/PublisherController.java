package org.joisen.realtime.publisher.controller;

import org.joisen.realtime.publisher.bean.NameValue;
import org.joisen.realtime.publisher.service.PublisherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @Author Joisen
 * @Date 2023/1/4 10:01
 * @Version 1.0
 */
@RestController
public class PublisherController {

    @Autowired
    PublisherService publisherService;

    /**
     * 交易分析  -   明细
     * http://bigdata.gmall.com/detailByItem?date=2023-01-03&itemName=小米手机&pageNo=1&pageSize=20
     * @param date
     * @param itemName
     * @param pageNo
     * @param pageSize
     * @return
     */
    @GetMapping("detailByItem")
    public Map<String, Object> detailByItem(@RequestParam("date") String date,
                                            @RequestParam("itemName") String itemName,
                                            @RequestParam(value = "pageNo", required = false, defaultValue = "1") Integer pageNo,
                                            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize){
        Map<String, Object> results = publisherService.doDetailByItem(date, itemName, pageNo, pageSize);
        return results;
    }

    /**
     * 交易分析 - 按照类别（年龄、性别）统计
     * http://bigdata.gmall.com/statsByItem?itemName=小米手机&date=2023-01-03&t=age
     * http://bigdata.gmall.com/statsByItem?itemName=小米手机&date=2023-01-04&t=gender
     * @param itemName
     * @param t
     * @return
     */
    @GetMapping("statsByItem")
    public List<NameValue> statsByItem(@RequestParam("itemName") String itemName,
                                       @RequestParam("date") String date,
                                       @RequestParam("t") String t){
        List<NameValue> results = publisherService.doStatsByItem(itemName, date, t);
        return results;
    }

    /**
     * 日活分析
     * http://bigdata.gmall.com/dauRealtime?td=2023-01-03
     * @param td
     * @return
     */
    @GetMapping("dauRealtime")
    public Map<String, Object> dauRealtime(@RequestParam("td") String td){

        Map<String, Object> result = publisherService.doDauRealtime(td);
        return result;
    }
}
