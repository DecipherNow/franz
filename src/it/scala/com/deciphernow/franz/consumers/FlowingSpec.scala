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

import java.util.concurrent.{CountDownLatch, Executors}

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.scaladsl.Producer
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import com.deciphernow.franz.KafkaDockerService
import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.scalatest.DockerTestKit
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord => Record}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.scalatest.{FunSpec, Matchers}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.Random

class FlowingSpec extends FunSpec with Matchers with DockerTestKit with DockerKitSpotify with KafkaDockerService {

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

  describe("Flowing.ephemeral") {

    describe("when the flow is synchronous") {

      it("emits all records to the flow in order") {

        fixture { (topic, values) =>

          val settings = consumerSettings.withGroupId(string(10))
          val subscription = Subscriptions.topics(topic)
          val actualValues = new ListBuffer[String]()
          val latch = new CountDownLatch(values.size)
          val flow = Flow[Record[String, String]].map { record =>
            actualValues += record.value
            latch.countDown()
            Done
          }
          val future = Flowing.ephemeral(settings, subscription, flow)

          latch.await()

          actualValues should contain theSameElementsInOrderAs values
        }
      }
    }

    describe("when the flow is asynchronous") {

      it("emits all records to the flow in any order") {

        fixture { (topic, values) =>

          val executionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(10))
          val settings = consumerSettings.withGroupId(string(10))
          val subscription = Subscriptions.topics(topic)
          val actualValues = new ListBuffer[String]()
          val latch = new CountDownLatch(values.size)
          val function = (record: Record[String, String]) => {
            Thread.sleep(Random.nextInt(1000))
            actualValues += record.value
            latch.countDown()
            Done
          }
          val flow = Flow[Record[String, String]].mapAsync(values.size)(r => Future(function(r))(executionContext))
          val future = Flowing.ephemeral(settings, subscription, flow)

          latch.await()

          actualValues should contain theSameElementsAs values
        }
      }
    }
  }

  describe("Flowing.persistent") {

    describe("when the flow is synchronous") {

      it("emits all records to the flow in order") {

        fixture { (topic, values) =>

          val settings = consumerSettings.withGroupId(string(10))
          val subscription = Subscriptions.topics(topic)
          val actualValues = new ListBuffer[String]()
          val latch = new CountDownLatch(values.size)
          val flow = Flow[Record[String, String]].map { record =>
            actualValues += record.value
            latch.countDown()
            Done
          }
          val future = Flowing.persistent(settings, subscription, flow, 1, 10, 1)

          latch.await()

          actualValues should contain theSameElementsInOrderAs values
        }
      }
    }

    describe("when the flow is asynchronous") {

      it("emits all records to the flow in any order") {

        fixture { (topic, values) =>

          val executionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(10))
          val settings = consumerSettings.withGroupId(string(10))
          val subscription = Subscriptions.topics(topic)
          val actualValues = new ListBuffer[String]()
          val latch = new CountDownLatch(values.size)
          val function = (record: Record[String, String]) => {
            Thread.sleep(Random.nextInt(1000))
            actualValues += record.value
            latch.countDown()
            Done
          }
          val flow = Flow[Record[String, String]].mapAsync(values.size)(r => Future(function(r))(executionContext))
          val future = Flowing.persistent(settings, subscription, flow, values.size, 10, 1)

          latch.await()

          actualValues should contain theSameElementsAs values
        }
      }
    }
  }
}
