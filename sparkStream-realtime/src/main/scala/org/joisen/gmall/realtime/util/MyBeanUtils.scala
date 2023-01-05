package org.joisen.gmall.realtime.util

import org.joisen.gmall.realtime.bean.{DauInfo, PageLog}

import java.lang.reflect.{Field, Method, Modifier}
import scala.util.control.Breaks

/**
 * @Author Joisen
 * @Date 2022/12/27 16:28
 * @Version 1.0
 */

/**
 * 实现对象属性拷贝
 */
object MyBeanUtils {
  /**
   * 将srcObj中属性的值拷贝到destObj对应的属性上。
   * @param srcObj
   * @param destObj
   */
  def copyProperties(srcObj: AnyRef, destObj: AnyRef): Unit = {
    if(srcObj == null || destObj == null){
      return
    }
    // 获取srcObj中所有的属性
    val srcFields: Array[Field] = srcObj.getClass.getDeclaredFields

    // 处理每个属性的拷贝
    for(srcField <- srcFields){
      Breaks.breakable{
        // Scala会自动为类中的属性提供get、set方法
        // get: fieldname()
        // set: fieldname_$eq(参数类型)
        // getMethodName
        var getMethodName: String = srcField.getName
        // setMethodName
        var setMethodName: String = srcField.getName + "_$eq"
        // 从srcObj中获取get方法对象
        val getMethod: Method = srcObj.getClass.getDeclaredMethod(getMethodName)
        // 从destObj中获取set方法对象
        // String name
        // getName()
        // setName(String name){ this.name = name }
        val setMethod: Method = 
        try {
          destObj.getClass.getDeclaredMethod(setMethodName, srcField.getType)
        } catch {
          case ex: Exception => Breaks.break()
        }

        // 忽略val属性
        val destField: Field = destObj.getClass.getDeclaredField(srcField.getName)
        if (destField.getModifiers.equals(Modifier.FINAL)){
          Breaks.break()
        }
        // 调用get方法获取到srcObj属性的值，再调用set方法将获取到的属性值赋值给destObj的属性
        setMethod.invoke(destObj, getMethod.invoke(srcObj))


      }// breakable


    }

  }

  def main(args: Array[String]): Unit = {
    val log: PageLog = PageLog("mid1001", "uid1001", "prov1001", null, null, null, null, null, null, null, null, null, null, 0L, null, 123456)
    val dauInfo = new DauInfo()
    println("拷贝前：" + dauInfo)

    copyProperties(log, dauInfo)
    println("拷贝后： " + dauInfo)


  }
}
