package org.joisen.gmall.realtime.app

import com.alibaba.fastjson.{JSON, JSONObject}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, OffsetRange}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.joisen.gmall.realtime.bean.{DauInfo, PageLog}
import org.joisen.gmall.realtime.util.{MyBeanUtils, MyEsUtils, MyKafkaUtils, MyOffsetUtils, MyRedisUtils}
import redis.clients.jedis.{Jedis, Pipeline}

import java.text.SimpleDateFormat
import java.time.{LocalDate, Period}
import java.{lang, util}
import java.util.Date
import scala.collection.mutable.ListBuffer

/**
 * @Author Joisen
 * @Date 2022/12/27 13:47
 * @Version 1.0
 */

/**
 * 日活宽表
 * 1. 准备实时环境
 * 2. 从redis中读取偏移量
 * 3. 从kafka中消费数据
 * 4. 提取偏移量结束点
 * 5. 处理数据
 *    5.1 转换数据结构
 *    5.2 去重
 *    5.3 维度关联
 * 6. 写入ES
 * 7. 提交offsets
 */
object DwdDauApp {
  def main(args: Array[String]): Unit = {
    // 还原状态
    revertState()

    // 准备实时环境
    val sparkConf: SparkConf = new SparkConf().setAppName("dwd_dau_app").setMaster("local[4]")
    val ssc: StreamingContext = new StreamingContext(sparkConf, Seconds(5))

    // 2. 从redis中读取offsets
    val topicName: String = "DWD_PAGE_LOG_TOPIC_1018"
    val groupId: String = "DWD_DAU_GROUP"
    val offsets: Map[TopicPartition, Long] = MyOffsetUtils.readOffset(topicName, groupId)
    // 3. 从kafka中消费数据
    var kafkaDStream: InputDStream[ConsumerRecord[String, String]] = null
    if (offsets != null && offsets.nonEmpty){
      kafkaDStream = MyKafkaUtils.getKafkaDStream(ssc, topicName, groupId, offsets)
    }else{
      kafkaDStream = MyKafkaUtils.getKafkaDStream(ssc, topicName, groupId)
    }

    // 提取offset结束点
    var offsetRanges: Array[OffsetRange] = null
    val offsetRangesDStream: DStream[ConsumerRecord[String, String]] = kafkaDStream.transform(
      rdd => {
        offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
        rdd
      }
    )

    // 5. 处理数据
    //  5.1 转换结构
    val pageLogDStream: DStream[PageLog] = offsetRangesDStream.map(
      consumerRecord => {
        val value: String = consumerRecord.value()
        val pageLog: PageLog = JSON.parseObject(value, classOf[PageLog])
        pageLog
      }
    )
    pageLogDStream.cache() // 避免经过这次处理后后续不能再对数据进行处理
    pageLogDStream.foreachRDD(
      rdd => println("自我审查前： " + rdd.count())
    )
    // 5.2 去重
    //    自我审查（将页面访问数据中last_page_id不为空的数据过滤掉）
    val filterDStream: DStream[PageLog] = pageLogDStream.filter(
      pageLog => pageLog.last_page_id == null
    )
    filterDStream.cache()
    filterDStream.foreachRDD(
      rdd => {
        println("自我审查后： " + rdd.count())
        println("-----------------")
      }
    )
    //    第三方审查（通过redis将当日活跃的mid维护起来，自我审查后的每条数据都需要到redis中进行比对去重）
    // redis如何维护日活状态
    //   类型：set
    //   key：DAU:DATE
    //   value：mid的集合
    //   写入Api：sadd
    //   读取Api：smembers
    //   过期：过期(24小时)
    /**
     * 获取数据的时间戳，转换成日期（yyyy-MM-dd），以日期为key，value为存储今日访问的用户mid的set存入redis中
     * 若当前数据的时间戳所对应的key中有当前数据的mid则过滤掉，
     */
    // filterDStream.filter() // 每条数据执行一次，redis的连接太频繁
    val redisFilterDStream: DStream[PageLog] = filterDStream.mapPartitions(
      pageLogIter => {
        val pageLogList: List[PageLog] = pageLogIter.toList
        println("第三方审查前： " + pageLogList.size)
        // 存储要的数据
        val pageLogs: ListBuffer[PageLog] = ListBuffer[PageLog]()
        val sdf = new SimpleDateFormat("yyyy-MM-dd")
        val jedis: Jedis = MyRedisUtils.getJedisFromPool()
        for (pageLog <- pageLogList) {
          // 提取每条数据中的mid（日活基于mid，也可以基于uid）
          val mid: String = pageLog.mid
          // 获取日期，因为要测试不同天的数据，所有不能直接获取系统事件
          val ts: Long = pageLog.ts
          val date: Date = new Date(ts)
          val dateStr: String = sdf.format(date)
          val redisDauKey: String = s"DAU:$dateStr"
          // redis的判断是否包含操作
          // 下面的代码在分布式环境中。存在并发问题，可能多个并行度同时进入到if中，导致最终保留多条同一个mid的数据
//          val setMids: util.Set[String] = jedis.smembers(redisDauKey)
//          if (!setMids.contains(mid)) {
//            jedis.sadd(redisDauKey, mid)
//            pageLogs.append(pageLog)
//          }
          val isNew: lang.Long = jedis.sadd(redisDauKey, mid) // 判断包含和写入实现了原子操作
          jedis.expire(redisDauKey, 24*3600) // 设置过期时间
          if(isNew == 1){
            pageLogs.append(pageLog)
          }
        }
        jedis.close()
        println("第三方审查后： " + pageLogs.size)
        pageLogs.iterator
      }
    )
//    redisFilterDStream.print()

    // 5.3 维度关联
    val dauInfoDStream: DStream[DauInfo] = redisFilterDStream.mapPartitions(
      pageLogIter => {
        val dauInfos: ListBuffer[DauInfo] = ListBuffer[DauInfo]()
        val sdf: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val jedis: Jedis = MyRedisUtils.getJedisFromPool()
        for (pageLog <- pageLogIter) {
          val dauInfo: DauInfo = new DauInfo()
          //1. 将pagelog中以后的字段拷贝到DauInfo中
          //笨办法: 将pageLog中的每个字段的值挨个提取，赋值给dauInfo中对应的字段。
          //dauInfo.mid = pageLog.mid
          //好办法: 通过对象拷贝来完成.
          MyBeanUtils.copyProperties(pageLog, dauInfo)

          //2. 补充维度
          //2.1  用户信息维度
          val uid: String = pageLog.user_id
          val redisUidKey: String = s"DIM:USER_INFO:$uid"
          val userInfoJson: String = jedis.get(redisUidKey)
          val userInfoJsonObj: JSONObject = JSON.parseObject(userInfoJson)
          //提取性别
          val gender: String = userInfoJsonObj.getString("gender")
          //提取生日
          val birthday: String = userInfoJsonObj.getString("birthday") // 1976-03-22
          //换算年龄
          val birthdayLd: LocalDate = LocalDate.parse(birthday)
          val nowLd: LocalDate = LocalDate.now()
          val period: Period = Period.between(birthdayLd, nowLd)
          val age: Int = period.getYears

          //补充到对象中
          dauInfo.user_gender = gender
          dauInfo.user_age = age.toString

          //2.2  地区信息维度
          // redis中:
          // 现在: DIM:BASE_PROVINCE:1
          // 之前: DIM:BASE_PROVINCE:110000
          val provinceID: String = dauInfo.province_id
          val redisProvinceKey: String = s"DIM:BASE_PROVINCE:$provinceID"
          val provinceJson: String = jedis.get(redisProvinceKey)
          val provinceJsonObj: JSONObject = JSON.parseObject(provinceJson)
          val provinceName: String = provinceJsonObj.getString("name")
          val provinceIsoCode: String = provinceJsonObj.getString("iso_code")
          val province3166: String = provinceJsonObj.getString("iso_3166_2")
          val provinceAreaCode: String = provinceJsonObj.getString("area_code")

          //补充到对象中
          dauInfo.province_name = provinceName
          dauInfo.province_iso_code = provinceIsoCode
          dauInfo.province_3166_2 = province3166
          dauInfo.province_area_code = provinceAreaCode

          //2.3  日期字段处理
          val date: Date = new Date(pageLog.ts)
          val dtHr: String = sdf.format(date)
          val dtHrArr: Array[String] = dtHr.split(" ")
          val dt: String = dtHrArr(0)
          val hr: String = dtHrArr(1).split(":")(0)
          //补充到对象中
          dauInfo.dt = dt
          dauInfo.hr = hr

          dauInfos.append(dauInfo)
        }

        jedis.close()
        dauInfos.iterator
      }
    )
//    dauInfoDStream.print(100)

    // 写入到OLAP中
    // 按照天分割索引，通过索引模板控制mapping、settings、aliases等
    // 准备ES工具类
    dauInfoDStream.foreachRDD(
      rdd => {
        rdd.foreachPartition(
          dauInfoIter =>{
            val docs: List[(String, DauInfo)] = dauInfoIter.map(dauInfo => (dauInfo.mid, dauInfo)).toList
            if(docs.nonEmpty){
              // 索引名
              // 由于是模拟数据，会生成不同天的数据
              // 从第一条数据中获取日期
              val head: (String, DauInfo) = docs.head
              val ts: Long = head._2.ts
              val sdf = new SimpleDateFormat("yyyy-MM-dd")
              val dataStr: String = sdf.format(new Date(ts))
              val indexName: String = s"gmall_dau_info_${dataStr}"
              // 写入到es中
              MyEsUtils.bulkSave(indexName, docs)
            }
          }
        )
        // 提交offset
        MyOffsetUtils.saveOffset(topicName, groupId, offsetRanges)
      }
    )



    ssc.start()
    ssc.awaitTermination()

  }

  /**
   * 状态还原
   *
   * 在每次启动实时任务时，进行一次状态还原， 以ES为准，将所有的mid提取出来，覆盖到Redis中。
   */
  def revertState(): Unit ={
    // 从ES中查询到所有的mid
    val date: LocalDate = LocalDate.now()
    val indexName: String = s"gmall_dau_info_$date"
    val fieldName: String = "mid"
    val mids: List[String] = MyEsUtils.searchField(indexName, fieldName)

    // 删除redis中记录的状态（所有的mid）
    val jedis: Jedis = MyRedisUtils.getJedisFromPool()
    val redisDauKey: String = s"DAU:$date"
    jedis.del(redisDauKey)

    // 将从ES中查询到的mid覆盖到Redis中
    if(mids != null && mids.nonEmpty){
//      for (mid <- mids) {
//        jedis.sadd(redisDauKey, mid)
//      }
      val pipeline: Pipeline = jedis.pipelined()
      for (mid <- mids) {
        pipeline.sadd(redisDauKey, mid) // 不会直接到redis执行
      }
      pipeline.sync() // 到redis执行
    }
    jedis.close()

  }


}
