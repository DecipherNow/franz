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
