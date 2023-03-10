package org.joisen.gmall.realtime.app

import com.alibaba.fastjson.serializer.SerializeConfig
import com.alibaba.fastjson.{JSON, JSONObject}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, OffsetRange}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.joisen.gmall.realtime.bean.{OrderDetail, OrderInfo, OrderWide}
import org.joisen.gmall.realtime.util.{MyEsUtils, MyKafkaUtils, MyOffsetUtils, MyRedisUtils}
import redis.clients.jedis.Jedis

import java.text.SimpleDateFormat
import java.time.{LocalDate, Period}
import java.util
import java.util.Date
import scala.collection.mutable.ListBuffer

/**
 * @Author Joisen
 * @Date 2022/12/28 13:07
 * @Version 1.0
 */

/**
 * 订单宽表任务
 * 1. 准备实时环境
 * 2. 从redis中读取offset
 * 3. 从kafka中消费数据
 * 4. 提取offset
 * 5. 数据处理
 *    5.1 转换结构
 *    5.2 维度关联
 *    5.3 双流join
 * 6. 写入ES
 * 7. 提交offset
 */
object DwdOrderApp {
  def main(args: Array[String]): Unit = {
    // 1 准备实时环境
    val sparkConf: SparkConf = new SparkConf().setAppName("dwd_order_app").setMaster("local[4]")
    val ssc = new StreamingContext(sparkConf, Seconds(5))

    // 2 读取offset
    // order_info
    val orderInfoTopicName: String = "DWD_ORDER_INFO_I"
    val orderInfoGroup: String = "DWD_ORDER_INFO:GROUP"
    val orderInfoOffsets: Map[TopicPartition, Long] = MyOffsetUtils.readOffset(orderInfoTopicName, orderInfoGroup)
    // order_detail
    val orderDetailTopicName: String = "DWD_ORDER_DETAIL_I"
    val orderDetailGroup: String = "DWD_ORDER_DETAIL:GROUP"
    val orderDetailOffsets: Map[TopicPartition, Long] = MyOffsetUtils.readOffset(orderDetailTopicName, orderDetailGroup)

    // 3 从kafka中消费数据
    // order_info
    var orderInfoKafkaDStream: InputDStream[ConsumerRecord[String, String]] = null
    if(orderInfoOffsets != null && orderInfoOffsets.nonEmpty){
      orderInfoKafkaDStream = MyKafkaUtils.getKafkaDStream(ssc, orderInfoTopicName, orderInfoGroup, orderInfoOffsets)
    }else{
      orderInfoKafkaDStream = MyKafkaUtils.getKafkaDStream(ssc, orderInfoTopicName, orderInfoGroup)
    }

    // order_detail
    var orderDetailKafkaDStream: InputDStream[ConsumerRecord[String, String]] = null
    if (orderDetailOffsets != null && orderDetailOffsets.nonEmpty) {
      orderDetailKafkaDStream = MyKafkaUtils.getKafkaDStream(ssc, orderDetailTopicName, orderDetailGroup, orderDetailOffsets)
    } else {
      orderDetailKafkaDStream = MyKafkaUtils.getKafkaDStream(ssc, orderDetailTopicName, orderDetailGroup)
    }

    // 4. 提取offset
    // order_info
    var orderInfoOffsetRanges: Array[OffsetRange] = null
    val orderInfoOffsetDStream: DStream[ConsumerRecord[String, String]] = orderInfoKafkaDStream.transform(
      rdd => {
        orderInfoOffsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
        rdd
      }
    )
    // order_detail 提取
    var orderDetailOffsetRanges: Array[OffsetRange] = null
    val orderDetailOffsetDStream: DStream[ConsumerRecord[String, String]] = orderDetailKafkaDStream.transform(
      rdd => {
        orderDetailOffsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
        rdd
      }
    )

    // 5.处理数据
    //   5.1 转换结构
    val orderInfoDStream: DStream[OrderInfo] = orderInfoOffsetDStream.map(
      consumerRecord => {
        val value: String = consumerRecord.value()
        val orderInfo: OrderInfo = JSON.parseObject(value, classOf[OrderInfo])
        orderInfo
      }
    )
//    orderInfoDStream.print(100)

    val orderDetailDStream: DStream[OrderDetail] = orderDetailOffsetDStream.map(
      consumerRecord => {
        val value: String = consumerRecord.value()
        val orderDetail: OrderDetail = JSON.parseObject(value, classOf[OrderDetail])
        orderDetail
      }
    )
//    orderDetailDStream.print(100)

    // 5.2 维度关联
    // order_info
    val orderInfoDimDStream: DStream[OrderInfo] = orderInfoDStream.mapPartitions(
      orderInfoIter => {
        // 不用维护list，改为将源iter转为list，将list中的元素进行更改再直接返回就行
        //        val orderInfos: ListBuffer[OrderInfo] = ListBuffer[OrderInfo]()
        val orderInfos: List[OrderInfo] = orderInfoIter.toList
        val jedis: Jedis = MyRedisUtils.getJedisFromPool()
        for (orderInfo <- orderInfos) {
          // 关联用户维度
          val uid: Long = orderInfo.user_id
          val redisUserKey: String = s"DIM:USER_INFO:$uid"
          val userInfoJson: String = jedis.get(redisUserKey)
          val userInfoJsonObj: JSONObject = JSON.parseObject(userInfoJson)
          // 提取性别
          val gender: String = userInfoJsonObj.getString("gender")
          // 提取生日
          val birthday: String = userInfoJsonObj.getString("birthday")
          // 换算年龄
          val birthdayLd: LocalDate = LocalDate.parse(birthday)
          val nowLd: LocalDate = LocalDate.now()
          val period: Period = Period.between(birthdayLd, nowLd)
          val age: Int = period.getYears

          // 补充到对象中
          orderInfo.user_age = age
          orderInfo.user_gender = gender

          // 关联地区维度
          val provinceId: Long = orderInfo.province_id
          val redisProvinceKey: String = s"DIM:BASE_PROVINCE:$provinceId"
          val provinceJson: String = jedis.get(redisProvinceKey)
          val provinceJsonObj: JSONObject = JSON.parseObject(provinceJson)

          val provinceName: String = provinceJsonObj.getString("name")
          val provinceAreaCode: String = provinceJsonObj.getString("area_code")
          val province3166: String = provinceJsonObj.getString("iso_3166_2")
          val provinceIsoCode: String = provinceJsonObj.getString("iso_code")
          // 补充到对象中
          orderInfo.province_name = provinceName
          orderInfo.province_area_code = provinceAreaCode
          orderInfo.province_3166_2_code = province3166
          orderInfo.province_iso_code = provinceIsoCode

          // 处理日期字段
          val createTime: String = orderInfo.create_time
          val createDtHr: Array[String] = createTime.split(" ")
          val createDate: String = createDtHr(0)
          val createHr: String = createDtHr(1).split(":")(0)
          // 补充到对象中
          orderInfo.create_date = createDate
          orderInfo.create_hour = createHr

          //          orderInfos.append(orderInfo)
        }
        jedis.close()
        orderInfos.iterator
      }
    )

//    orderInfoDimDStream.print(100)

    // 5.3 双流join
    //    内连接 join  结果集去交集
    //    外连接
    //        左外连  leftOuterJoin   结果集取左表的所有+右表的匹配
    //        右外连  rightOuterJoin  结果集取右表的所有+左表的匹配
    //        全外连  fullOuterJoin   结果集取两张表的所有

    // 从数据库层面： order_info 表中的数据 和 order_detail表中的数据一定能关联成功
    // 从流处理层面： order_info 和 order_detail是两个流，流的join只能是同一个批次的数据才能进行join
    //              如果两个表的数据进入到不同的批次，就会join不成功
    //              数据延迟导致的数据没有进入到同一个批次，再实时处理中是正常现象，我们可以接受因为延迟导致最终的结果延迟，但是不能接受因为延迟导致的数据丢失
    val orderInfoKVDStream: DStream[(Long, OrderInfo)] = orderInfoDimDStream.map(orderInfo => (orderInfo.id, orderInfo))

    val orderDetailKVDStream: DStream[(Long, OrderDetail)] = orderDetailDStream.map(orderDetail => (orderDetail.order_id, orderDetail))
//    val orderJoinDStream: DStream[(Long, (OrderInfo, OrderDetail))] = orderInfoKVDStream.join(orderDetailKVDStream) // 这种join方式会出现因为延迟而导致丢失数据的问题（丢掉没有匹配上的数据）
    // 解决办法： 1、扩大采集周期，治标不治本
    //          2、使用窗口，治标不治本，还要考虑数据去重、Spark状态的缺点
    //          3、首先使用fullOuterJoin，保证join成功或者没有成功的数据都出现再结果中。
    //             让双方都多两步操作，到缓存中找对的数据，或者存入缓存中等待匹配数据
    val orderJoinDStream: DStream[(Long, (Option[OrderInfo], Option[OrderDetail]))] = orderInfoKVDStream.fullOuterJoin(orderDetailKVDStream)

    val orderWideDStream: DStream[OrderWide] = orderJoinDStream.mapPartitions(
      orderJoinIter => {
        val jedis: Jedis = MyRedisUtils.getJedisFromPool()
        val orderWides: ListBuffer[OrderWide] = ListBuffer[OrderWide]()
        for ((key, (orderInfoOp, orderDetailOp)) <- orderJoinIter) {
          // orderInfo有，orderDetail有
          if (orderInfoOp.isDefined) { // orderInfo有，orderDetail也有，则组装成orderWide，然后orderInfo写入缓存，再读缓存中是否有orderDetail
            // 取出OrderInfo
            val orderInfo: OrderInfo = orderInfoOp.get
            if (orderDetailOp.isDefined) {
              // 取出orderDetail
              val orderDetail: OrderDetail = orderDetailOp.get
              // 组装成orderWide
              val orderWide = new OrderWide(orderInfo, orderDetail)
              // 放入到结果集中
              orderWides.append(orderWide)
            }
            // orderInfo有，orderDetail没有


            // orderInfo写缓存
            // 类型：string
            // key：ORDERJOIN:ORDER_INFO:ID
            // value：json
            // 写入Api：set
            // 读取Api：get
            // 是否过期: 24小时
            val redisOrderInfoKey: String = s"ORDERJOIN:ORDER_INFO:${orderInfo.id}"
            jedis.setex(redisOrderInfoKey, 24 * 3600, JSON.toJSONString(orderInfo, new SerializeConfig(true)))

            // orderInfo读缓存
            val redisOrderDetailKey: String = s"ORDERJOIN:ORDER_DETAIL:${orderInfo.id}"
            val orderDetails: util.Set[String] = jedis.smembers(redisOrderDetailKey)
            if (orderDetails != null && orderDetails.size() > 0) {
              import scala.collection.JavaConverters._
              for (orderDetailJson <- orderDetails.asScala) {
                val orderDetail: OrderDetail = JSON.parseObject(orderDetailJson, classOf[OrderDetail])
                // 组装成orderWide
                val orderWide = new OrderWide(orderInfo, orderDetail)
                // 加入到结果集中
                orderWides.append(orderWide)
              }
            }

          } else {
            // orderInfo没有，orderDetail有
            val orderDetail: OrderDetail = orderDetailOp.get
            // 读缓存
            val redisOrderInfoKey: String = s"ORDERJOIN:ORDER_INFO:${orderDetail.order_id}"
            val orderInfoJson: String = jedis.get(redisOrderInfoKey)
            if (orderInfoJson != null && orderInfoJson.nonEmpty) {
              val orderInfo: OrderInfo = JSON.parseObject(orderInfoJson, classOf[OrderInfo])
              // 组装成orderWide
              val orderWide: OrderWide = new OrderWide(orderInfo, orderDetail)
              // 加入到结果集中
              orderWides.append(orderWide)
            } else {
              // 写缓存
              // 类型：set
              // key：ORDERJOIN:ORDER_DETAIL:ORDER_ID
              // value：json
              // 写入Api：sadd
              // 读取Api：smembers
              // 是否过期: 24小时
              val redisOrderDetailKey: String = s"ORDERJOIN:ORDER_DETAIL:${orderDetail.order_id}"
              jedis.sadd(redisOrderDetailKey, JSON.toJSONString(orderDetail, new SerializeConfig(true)))
              jedis.expire(redisOrderDetailKey, 24 * 3600)

            }

          }


        }
        jedis.close()
        orderWides.iterator
      }
    )

//    orderWideDStream.print(1000)


//    orderJoinDStream.print(1000)
    // 写入ES
    // 1 索引分割，通过索引模板控制mapping setting aliases
    // 2 使用工具类将数据写入ES
    orderWideDStream.foreachRDD(
      rdd=>{
        rdd.foreachPartition(
          orderWideIter =>{
            val docs: List[(String, OrderWide)] = orderWideIter.map(orderWide => (orderWide.order_id.toString, orderWide)).toList
            if(docs.nonEmpty){
              // 从第一条数据中获取日期
              val head: (String, OrderWide) = docs.head
              val date: String = head._2.create_date
              val indexName: String = s"gmall_order_wide_$date"
              // 写入ES
              MyEsUtils.bulkSave(indexName, docs)
            }
          }
        )
        // 提交offset
        MyOffsetUtils.saveOffset(orderInfoTopicName, orderInfoGroup, orderInfoOffsetRanges)
        MyOffsetUtils.saveOffset(orderDetailTopicName, orderDetailGroup, orderDetailOffsetRanges)
      }
    )


    ssc.start()
    ssc.awaitTermination()
  }
}
