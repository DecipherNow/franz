# Franz

A library for working with Kafka via Akka streams.

## Usage

To use Franz within a Maven project add the following dependency:

```xml

<dependency>
    <groupId>com.deciphernow</groupId>
    <artifactId>franz</artifactId>
    <version>1.0.0</version>
</dependency>

```

To use Franz within an SBT project add the following dependency:

```sbtshell
libraryDependencies += "com.deciphernow" % "franz" % "1.0.0"
```

### Consuming from Kafka

Franz supports two methods of consuming records from Kafka: fetching and flowing. Fetching provides a means to retrieve a set number of records from Kafka while flowing allows you to continually stream records from Kafka. In most production use cases, you will be flowing from Kafka, but in a limited set of cases (e.g., testing) you simply need to fetch from Kafka. In either case Franz supports ephemeral consumers (i.e. consumers that do not track offsets) and persistent consumers (i.e., consumers that track offsets).

#### Fetching

The following line demonstrates how to fetch from Kafka:

```scala
Fetching.ephemera(settings, subscription, 10, Duration(10, SECONDS))
```

The above code will return ten records from Kafka or, if ten records are not available after waiting ten seconds, it will return all available records.  To execute the same action but persist the offsets to Kafka so that subsequent calls do not retrieve the same records do the following:

```scala
Fetching.persistent(settings, subscription, 10, Duration(10, SECONDS))
```

#### Flowing

To flow records from Kafka do the following:

```scala
val flow = Flow[ConsumerRecord[K, V]].map { record =>
  /* Process the record here */
  Done
}

val future = Flowing.ephemeral(settings, subscription, flow)
```

If you desire to process records in parallel simply modify your flow to something like:

```scala
val flow = Flow[ConsumerRecord[K, V]].mapAsync(10) { record =>
  Future {
    /* Process the record here */
    Done
  }
}
```

This will allow for up to ten records to be processed in parallel.

If you need to commit offsets back to Kafka (in most use cases this is required) you should do the following:

```scala
val flow = Flow[ConsumerRecord[K, V]].map { record =>
  /* Process the record here */
  Done
}

val future = Flowing.persistent(settings, subscription, flow, 1, 10, 1)
```

The above will process one record at a time with the provided flow, but allows for commits to be batched in groups of up to ten records which are processed in a single thread. You can also process records in parallel but must ensure that the level of parallelism of the provided flow matches the `flowParallelism` parameter passed to `Flowing.persistent`. Failure to do so will result in either a blocking flow or buffer overflow.


```scala
val flow = Flow[ConsumerRecord[K, V]].mapAsync(25) { record =>
  /* Process the record here */
  Done
}

val future = Flowing.persistent(settings, subscription, flow, 25, 10, 1)
```

Note that commiting records to Kafka is expensive and allowing for larger batches or more parallelism will speed up your flow. However, larger batches may aslo result in more messages being reprocessed in failure cases.

# Contributing

1. Fork it
1. Create your feature branch (git checkout -b my-new-feature)
1. Commit your changes (git commit -am 'Add some feature')
1. Push to the branch (git push origin my-new-feature)
1. Create new Pull Request
