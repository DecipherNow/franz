package com.deciphernow.franz.internal

import akka.event.Logging
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

final case class Block[T](count: Long) extends GraphStage[FlowShape[T, T]] {

  val in = Inlet[T](s"${Logging.simpleName(this)}.in")
  val out = Outlet[T](s"${Logging.simpleName(this)}.out")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {

    new GraphStageLogic(shape) with InHandler with OutHandler {

      private var remaining = count

      override def onPush(): Unit = {
        if (remaining > 0) {
          push(out, grab(in))
          remaining -= 1
        }
      }

      override def onPull(): Unit = {
        if (remaining > 0) {
          pull(in)
        }
      }

      setHandlers(in, out, this)
    }
  }

  override def shape: FlowShape[T, T] = {
    FlowShape(in, out)
  }

  override def toString: String = {
    "Block"
  }
}
