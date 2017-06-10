/*
 * Copyright 2017 Decipher Technology Studios LLC
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

package com.deciphernow.franz.consumers

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Producer
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.deciphernow.franz.KafkaDockerService
import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.scalatest.DockerTestKit
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, MILLISECONDS}
import scala.util.Random

class FetchingSpec extends FunSpec with Matchers with DockerTestKit with DockerKitSpotify with KafkaDockerService {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()(system)

  private val deserializer = new StringDeserializer()
  private val serializer = new StringSerializer()

  private val consumerSettings = ConsumerSettings(system, deserializer, deserializer)
    .withBootstrapServers("kafka.docker:9092")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")

  private val producerSettings = ProducerSettings(system, serializer, serializer)
    .withBootstrapServers("kafka.docker:9092")

  def string(length: Int): String = {
    Random.alphanumeric.take(10).mkString
  }

  def fixture(test: (String, Seq[String]) => Unit): Unit = {
    val topic = string(10)
    val key = string(10)
    val values = (1 to 10).map(_ => string(10))
    val records = values.map(value => new ProducerRecord[String, String](topic, key, value))

    Await.ready(Source(records).runWith(Producer.plainSink(producerSettings)), Duration.Inf)

    test(topic, values)
  }

  describe("Fetching.ephemeral") {

    describe("when the number of records requested exceeds the records available") {

      it("should return the available records") {

        fixture { (topic, values) =>

          val count = values.size + 1
          val settings = consumerSettings.withGroupId(string(10))
          val subscription = Subscriptions.topics(topic)
          val duration = Duration(2500, MILLISECONDS)

          val records = Await.result(Fetching.ephemeral(settings, subscription, count, duration), Duration.Inf)

          val expectedValues = values
          val actualValues = records.map(_.value)

          actualValues should contain theSameElementsInOrderAs values
        }
      }
    }

    describe("when the number of records available exceeds the records requested") {

      it("should return the requested records") {

        fixture { (topic, values) =>

          val count = values.size + 1
          val settings = consumerSettings.withGroupId(string(10))
          val subscription = Subscriptions.topics(topic)
          val duration = Duration(2500, MILLISECONDS)

          val records = Await.result(Fetching.ephemeral(settings, subscription, count, duration), Duration.Inf)

          val expectedValues = values
          val actualValues = records.map(_.value)

          actualValues should contain theSameElementsInOrderAs values
        }
      }
    }

    describe("when restarted") {

      it("should reprocess the same records") {

        fixture { (topic, values) =>

          val count = (values.size * 0.5).toInt
          val settings = consumerSettings.withGroupId(string(10))
          val subscription = Subscriptions.topics(topic)
          val duration = Duration(2500, MILLISECONDS)

          val firstRecords = Await.result(Fetching.ephemeral(settings, subscription, count, duration), Duration.Inf)
          val secondRecords = Await.result(Fetching.ephemeral(settings, subscription, count, duration), Duration.Inf)

          val firstValues = firstRecords.map(_.value)
          val secondValues = secondRecords.map(_.value)

          firstValues should contain theSameElementsInOrderAs secondValues
        }
      }
    }
  }

  describe("Fetching.persistent") {

    describe("when the number of records requested exceeds the records available") {

      it("should return the available records") {
        fixture { (topic, values) =>

          val count = values.size - 1
          val settings = consumerSettings.withGroupId(string(10))
          val subscription = Subscriptions.topics(topic)
          val duration = Duration(2500, MILLISECONDS)
          val records = Await.result(Fetching.persistent(settings, subscription, count, duration), Duration.Inf)

          val expectedValues = values.slice(0, count)
          val actualValues = records.map(_.value)

          actualValues should contain theSameElementsInOrderAs expectedValues
        }
      }
    }

    describe("when the number of records available exceeds the records requested") {

      it("should return the requested records") {

        fixture { (topic, values) =>

          val count = values.size - 1
          val settings = consumerSettings.withGroupId(string(10))
          val subscription = Subscriptions.topics(topic)
          val duration = Duration(2500, MILLISECONDS)
          val records = Await.result(Fetching.persistent(settings, subscription, count, duration), Duration.Inf)

          val expectedValues = values.slice(0, count)
          val actualValues = records.map(_.value)

          actualValues should contain theSameElementsInOrderAs expectedValues
        }
      }
    }

    describe("when restarted") {

      it("should not reprocess the same records") {

        fixture { (topic, values) =>

          val count = (values.size * 0.5).toInt
          val settings = consumerSettings.withGroupId(string(10))
          val subscription = Subscriptions.topics(topic)
          val duration = Duration(2500, MILLISECONDS)

          val firstRecords = Await.result(Fetching.persistent(settings, subscription, count, duration), Duration.Inf)
          val secondRecords = Await.result(Fetching.persistent(settings, subscription, count, duration), Duration.Inf)

          val firstValues = firstRecords.map(_.value)
          val secondValues = secondRecords.map(_.value)

          firstValues should contain theSameElementsInOrderAs values.slice(0, count)
          secondValues should contain theSameElementsInOrderAs values.slice(count, values.size)
        }
      }
    }
  }
}
