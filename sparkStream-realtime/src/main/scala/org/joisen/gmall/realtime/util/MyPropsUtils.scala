package org.joisen.gmall.realtime.util

import java.util.ResourceBundle

/**
 * 配置文件解析类
 *
 * @Author Joisen
 * @Date 2022/12/23 19:04
 * @Version 1.0
 */
object MyPropsUtils {
  private val bundle: ResourceBundle = ResourceBundle.getBundle("config")

  def apply(propsKey: String): String= {
    bundle.getString(propsKey)
  }

  def main(args: Array[String]): Unit = {
    println(MyPropsUtils("kafka.bootstrap-servers"))
  }

}
