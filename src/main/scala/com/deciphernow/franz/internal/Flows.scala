package com.deciphernow.franz.internal

import akka.kafka.ConsumerMessage.{CommittableMessage => Message, CommittableOffsetBatch => Batch}
import akka.stream.scaladsl.GraphDSL.Implicits.{flow2flow, port2flow}
import akka.stream.scaladsl.{Flow, GraphDSL, UnzipWith, ZipWith}
import akka.stream.{FlowShape, OverflowStrategy}
import akka.{Done, NotUsed}
import org.apache.kafka.clients.consumer.{ConsumerRecord => Record}

object Flows {

  def committable[K, V, M](flow: Flow[Record[K, V], Done, NotUsed],
                           flowParallelism: Int): Flow[Message[K, V], Message[K, V], NotUsed] = {
    val graph = GraphDSL.create(flow) { implicit builder => flow =>

      val unzip = builder.add(UnzipWith[Message[K, V], Record[K, V], Message[K, V]] { message =>
        (message.record, message)
      })

      val buffer = builder.add(Flow[Message[K, V]].buffer(flowParallelism, OverflowStrategy.fail))

      val zip = builder.add(ZipWith[Done, Message[K, V], Message[K, V]] { (_, message) =>
        message
      })

      unzip.out0 ~>  flow  ~> zip.in0
      unzip.out1 ~> buffer ~> zip.in1

      FlowShape(unzip.in, zip.out)
    }
    Flow.fromGraph(graph)
  }

  def committing[K, V](commitSize: Int,
                       commitParallelism: Int): Flow[Message[K, V], Message[K, V], NotUsed] = {
    val graph = GraphDSL.create() { implicit builder =>

      val batch = builder.add(Flow[Message[K, V]].batch(commitSize, (message) => List(message)) { (seed, message) =>
        seed :+ message
      })

      val unzip = builder.add(UnzipWith[List[Message[K, V]], Batch, List[Message[K, V]]] { messages =>
        (messages.foldLeft(Batch.empty)((batch, message) => batch.updated(message.committableOffset)), messages)
      })

      val commit = builder.add(Flow[Batch].mapAsync(commitParallelism) { batch =>
        batch.commitScaladsl
      })

      val buffer = builder.add(Flow[List[Message[K, V]]].buffer(commitParallelism, OverflowStrategy.fail))

      val zip = builder.add(ZipWith[Done, List[Message[K, V]], List[Message[K, V]]] { (_, messages) =>
        messages
      })

      val flatten = builder.add(Flow[List[Message[K, V]]].mapConcat(identity))

      batch.out ~> unzip.in
                   unzip.out0 ~> commit ~> zip.in0
                   unzip.out1 ~> buffer ~> zip.in1
                                           zip.out ~> flatten

      FlowShape(batch.in, flatten.out)
    }
    Flow.fromGraph(graph)
  }
}
