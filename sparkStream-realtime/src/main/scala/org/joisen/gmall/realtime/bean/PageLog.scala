package org.joisen.gmall.realtime.bean

/**
 * @Author Joisen
 * @Date 2022/12/24 13:42
 * @Version 1.0
 */
case class PageLog(
                    mid: String,
                    user_id: String,
                    province_id: String,
                    channel: String,
                    is_new: String,
                    model: String,
                    operate_system: String,
                    version_code: String,
                    brand: String,
                    page_id: String,
                    last_page_id: String,
                    page_item: String,
                    page_item_type: String,
                    during_time: Long,
                    sourceType: String,
                    ts: Long
                  )
