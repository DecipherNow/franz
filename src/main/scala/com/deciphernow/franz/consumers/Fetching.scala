package com.deciphernow.franz.consumers

import akka.kafka.{ConsumerSettings, Subscription}
import akka.kafka.scaladsl.Consumer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.deciphernow.franz.internal.Implicits.{BlockingSource, CommittableSource}
import org.apache.kafka.clients.consumer.{ConsumerRecord => Record}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

object Fetching {

  def ephemeral[K, V](settings: ConsumerSettings[K, V],
                      subscription: Subscription,
                      count: Int,
                      timeout: FiniteDuration)
                     (implicit materializer: ActorMaterializer): Future[Seq[Record[K, V]]] = {
    Consumer.plainSource(settings, subscription)
      .take(count)
      .takeWithin(timeout)
      .runWith(Sink.seq[Record[K, V]])
  }

  def persistent[K, V](settings: ConsumerSettings[K, V],
                       subscription: Subscription,
                       count: Int,
                       timeout: FiniteDuration)
                      (implicit materializer: ActorMaterializer): Future[Seq[Record[K, V]]] = {
    Consumer.committableSource(settings, subscription)
      .block(count)
      .blockWithin(timeout)
      .commit(1, 1)
      .take(count)
      .takeWithin(timeout)
      .map(_.record)
      .runWith(Sink.seq[Record[K, V]])
  }
}
