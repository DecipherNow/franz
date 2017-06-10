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
