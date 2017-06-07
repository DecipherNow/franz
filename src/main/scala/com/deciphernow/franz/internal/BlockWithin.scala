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
