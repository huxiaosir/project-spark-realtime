package org.joisen.gmall.realtime.util

/**
 * @Author Joisen
 * @Date 2022/12/23 19:10
 * @Version 1.0
 */
object MyConfig {
  val KAFKA_BOOTSTRAP_SERVERS: String = "kafka.bootstrap-servers"
  val KAFKA_STRING_DESERIALIZATION: String = "kafka.string_deserialization"
  val KAFKA_STRING_SERIALIZATION: String = "kafka.string_serialization"

  val REDIS_HOST: String = "redis.host"
  val REDIS_PORT: String = "redis.port"

  val ES_HOST: String = "es.host"
  val ES_PORT: String = "es.port"
}
