Rekafka
===
Simple relay for moving messages from one Kafka topic to another, across different brokers if need be. Rekafka gives you a controlled way of copying messages; running either as a daemon - copying messages as they arrive, or copying a batch of messages at a time.

# Running

## Step 1: build the Jar
This assumes you have cloned the repo and have JDK 8 installed.
```sh
./gradlew build
```

## Step 2: create a config
A Rekafka config essentially describes a replication pipe with two ends - the source and the sink. Each end has a `topic` parameter and a set of `properties` controlling the Kafka consumer (source end) or the producer (sink end).

Use `sample.yaml` to get started, shown below for convenience. Provide your own values as appropriate.
```yaml
source:
  topic: platform.push
  properties:
    zookeeper.connect: sitapzoo:2181
    group.id: relay
    auto.commit.enable: true
    auto.offset.reset: smallest
sink:
  topic: platform.push
  properties:
    metadata.broker.list: 10.141.64.100:9092,10.141.68.100:9092,10.141.72.100:9092
```

## Step 3: run it
```sh
java -jar build/libs/rekafka-full-0.1.0-SNAPSHOT.jar --conf config.yaml
```

Replace `rekafka-full-0.1.0-SNAPSHOT.jar` with the appropriate version.

By default, the application runs as a daemon - starting from the earliest known offset and copying all messages until it reaches the end of the topic, at which point it will await new messages and copy them as they arrive.

To copy only a subset of messages, run the application with the `--max-records` option, passing in the number of records to copy. When the target number is reached, the application will terminate. This is useful for testing, when you might only need a handful of messages at a time, or if you don't want to expose the ultimate consumer application to a firehose of messages.

# Logging
The logging configuration is currently in `src/main/resources/log4j.properties`; it's fine-grained by default and only prints to the console. To change this, either recompile the application with your `log4j.properties` file, or pass in an alternative Log4j configuration on startup, as so:
```sh
java -jar rekafka-full-0.1.0-SNAPSHOT.jar -Dlog4j.configuration=file:log4j-custom.properties
```