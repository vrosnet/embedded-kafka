/*
 * Copyright 2016 Tuplejump
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tuplejump.embedded.kafka

import java.util.concurrent.atomic.AtomicInteger

import org.apache.kafka.common.serialization.StringDeserializer
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Millis, Span}

class InitialSpec extends AbstractSpec with Eventually with Logging {

  private val timeout = Timeout(Span(10000, Millis))

  "Initially, EmbeddedKafka" must {
    val kafka = new EmbeddedKafka()
    val topic = "test"
    val atomic = new AtomicInteger(0)
    val batch1 = for (n <- 0 until 1000) yield s"message-test-$n"

    "start embedded zookeeper and embedded kafka" in {
      kafka.isRunning should be (false)
      kafka.start()
      eventually(timeout)(kafka.isRunning)
    }
    "create a topic" in {
      kafka.createTopic(topic, 1, 1)
    }
    "publish messages to the embedded kafka instance" in {
      val config = kafka.consumerConfig(
        group = "some.group",
        kafkaConnect = DefaultKafkaConnect,
        zkConnect = DefaultZookeeperConnect,
        offsetPolicy = "largest",//different with new consumer.
        autoCommitEnabled = true,
        kDeserializer = classOf[StringDeserializer],
        vDeserializer = classOf[StringDeserializer])
      val consumer = new SimpleConsumer(atomic, config, topic, "consumer.group", 1, 1)

      kafka.sendMessages(topic, batch1)
      logger.info(s"Publishing ${batch1.size} messages...")
      eventually(timeout)(consumer.count.get >= batch1.size)

      consumer.shutdown()
    }
    "shut down relatively cleanly for now" in {
      kafka.shutdown()
      eventually(timeout)(!kafka.isRunning)
    }
  }
}
