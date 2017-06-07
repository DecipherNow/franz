package com.deciphernow.franz.consumers

import akka.{Done, NotUsed}
import akka.kafka.{ConsumerSettings, Subscription}
import akka.kafka.scaladsl.Consumer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}
import com.deciphernow.franz.internal.Flows
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.Future

object Flowing {

  def ephemeral[K, V](settings: ConsumerSettings[K, V],
                      subscription: Subscription,
                      flow: Flow[ConsumerRecord[K, V], Done, NotUsed])
                     (implicit materializer: ActorMaterializer): Future[Done] = {
    Consumer.plainSource(settings, subscription)
      .via(flow)
      .runWith(Sink.ignore)
  }

  def persistent[K, V](settings: ConsumerSettings[K, V],
                       subscription: Subscription,
                       flow: Flow[ConsumerRecord[K, V], Done, NotUsed],
                       flowParallelism: Int,
                       commitSize: Int,
                       commitParallelism: Int)
                      (implicit materializer: ActorMaterializer): Future[Done] = {
    Consumer.committableSource(settings, subscription)
      .via(Flows.committable(flow, flowParallelism))
      .via(Flows.committing(commitSize, commitParallelism))
      .runWith(Sink.ignore)
  }


}
