package com.deciphernow.franz.internal

import akka.kafka.ConsumerMessage.{CommittableMessage => Message}
import akka.stream.scaladsl.Source

import scala.concurrent.duration.FiniteDuration

object Implicits {

  implicit class BlockingSource[T, M](source: Source[T, M]) {

    def block(count: Long): Source[T, M] = {
      source.via(Block(count))
    }

    def blockWithin(timeout: FiniteDuration): Source[T, M] = {
      source.via(BlockWithin(timeout))
    }
  }

  implicit class CommittableSource[K, V, M](source: Source[Message[K, V], M]) {

    def commit(size: Int, parallelism: Int): Source[Message[K, V], M] = {
      source.via(Flows.committing(size, parallelism))
    }
  }
}
