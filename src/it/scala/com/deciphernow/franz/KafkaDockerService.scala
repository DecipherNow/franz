package com.deciphernow.franz

import com.whisk.docker.{DockerContainer, DockerKit, DockerReadyChecker}

trait KafkaDockerService extends DockerKit {

  val kafkaContainer = DockerContainer("deciphernow/kafka:2.11-0.10.0.0")
    .withHostname("kafka.docker")
    .withPorts(9092 -> Some(9092), 2181 -> Some(2181))
    .withEnv("KAFKA_HOST=kafka.docker", "KAFKA_PORT=9092", "ZOOKEEPER_HOST=kafka.docker", "ZOOKEEPER_PORT=2181")
    .withReadyChecker(DockerReadyChecker.LogLineContains("Registered broker 0 at path /brokers/ids/0"))

  abstract override def dockerContainers: List[DockerContainer] = kafkaContainer :: super.dockerContainers

}
