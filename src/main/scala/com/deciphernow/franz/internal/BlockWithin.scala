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

import akka.event.Logging
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage._

import scala.concurrent.duration.FiniteDuration

final case class BlockWithin[T](timeout: FiniteDuration) extends GraphStage[FlowShape[T, T]] {

  val in = Inlet[T](s"${Logging.simpleName(this)}.in")
  val out = Outlet[T](s"${Logging.simpleName(this)}.out")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {

    new TimerGraphStageLogic(shape) with InHandler with OutHandler {

      private var block = false

      def onPush(): Unit = {
        if (!block) {
          push(out, grab(in))
        }
      }

      def onPull(): Unit = {
        if (!block) {
          pull(in)
        }
      }

      setHandlers(in, out, this)

      final override protected def onTimer(key: Any): Unit = {
        block = true
      }

      override def preStart(): Unit = {
        scheduleOnce("BlockWithinTimer", timeout)
      }
    }
  }

  override def shape: FlowShape[T, T] = {
    FlowShape(in, out)
  }

  override def toString: String = {
    "BlockWithin"
  }

}
