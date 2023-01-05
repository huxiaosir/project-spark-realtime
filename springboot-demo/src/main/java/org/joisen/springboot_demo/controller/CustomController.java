package org.joisen.springboot_demo.controller;

import org.joisen.springboot_demo.bean.Custom;
import org.joisen.springboot_demo.service.CustomService;
import org.joisen.springboot_demo.service.impl.CustomServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * @Author Joisen
 * @Date 2023/1/3 13:51
 * @Version 1.0
 */
//@Controller
@RestController  // @Controller + @ResponseBody
public class CustomController {

    @Autowired
    CustomService customService;

    // http://localhost:8080/login?username=zhangsan&password=123456
    @GetMapping("login")
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password){
        // 在每个方法中创建业务层对象， 不推荐
//        CustomServiceImpl customServiceImpl = new CustomServiceImpl();

        return customService.doLogin(username, password);
    }

    /**
     * 常见状态码：
     *          200：表示请求处理成功且响应成功
     *          302：表示进行重定向
     *          400：表示请求参数有误
     *          404：表示请求地址或者资源不存在
     *          405：表示请求方式不支持
     *          500：表示服务器端处理异常
     *
     * http://localhost:8080/statuscode
     */
    @GetMapping("statuscode")
    public String statusCode(@RequestParam("username") String username, @RequestParam("age") Integer age){
        // 测试代码 400
        return username + " -> " + age;
    }

    /**
     * GET: 读
     * POST: 写
     * http://localhost:8080/requestmethod
     */
    @RequestMapping(value = "requestmethod", method = RequestMethod.GET)
//    @PostMapping("requestmethod")
//    @GetMapping("requestmethod")
    public String requestMethod(){
        return "success";
    }

    /**
     * 请求参数：
     *      1. 地址栏中的kv格式的参数
     *      2. 嵌入到地址栏中的参数
     *      3. 封装到请求体中的参数
     */
    /**
     * 1. 地址栏中的kv格式的参数
     *  localhost:8080/paramkv?username=zhangsan&age=22
     *  如果请求参数名与方法形参名一致，可以直接进行参数值的映射(不需要在参数前面加注解 @RequestParam)
     *  @RequestParam ： 将请求参数映射到方法对应的形参上
     */
    @RequestMapping("paramkv")
    public String paramKv(@RequestParam("username") String name, @RequestParam("age")Integer age){

        return "name = " + name + ", age = " + age;
    }

    /**
     * 2. 嵌入到地址栏中的参数
     * http://localhost:8080/parampath/lisi/23?address=beijing
     * @PathVariable : 将请求路径中的参数映射到请求方法对应的形参上。
     */
    @RequestMapping("parampath/{username}/{age}")
    public String paramPath(@PathVariable("username") String name, @PathVariable("age") Integer age, @RequestParam("address") String address){
        return "name = " + name + ", age = " + age + ",  address = " + address;
    }

    /**
     * 3. 封装到请求体中的参数
     *  http://localhost:8080/parambody
     *  请求体中的参数：
     *      uname=xxx
     *      password=xxx
     *  如果请求的参数名与方法的形参名不一致，需要@RequestParam来标识获取
     *  如果一致，可以直接映射
     *
     * @RequestBody : 将请求体中的json格式的参数映射到对象对应的属性上。
     */
//    @RequestMapping("parambody")
//    public String paramBody(String uname, String password){
//        return "uname = " + uname + ", password = " + password;
//    }
    @RequestMapping("parambody")
    public Custom paramBody(@RequestBody Custom custom){
        return custom; // 转换成json字符串返回
    }


    /**
     * 客户端请求：http://localhost:8080/helloworld
     * @RequestMapping : 将客户端请求与方法进行映射
     * @ResponseBody : 将方法的返回值处理成字符串(json)返回给客户端
     */
    @RequestMapping("helloworld")
//    @ResponseBody
    public String helloWord(){

        return "success";
    }


    @RequestMapping("hello")
//    @ResponseBody
    public String hello(){

        return "hello java";
    }
}
