package org.joisen.gmall.realtime.bean

/**
 * @Author Joisen
 * @Date 2022/12/28 13:41
 * @Version 1.0
 */
case class OrderDetail(
                        id: Long,
                        order_id: Long,
                        sku_id: Long,
                        order_price: Double,
                        sku_num: Long,
                        sku_name: String,
                        create_time: String,
                        split_total_amount: Double = 0D,
                        split_activity_amount: Double = 0D,
                        split_coupon_amount: Double = 0D
                      )
