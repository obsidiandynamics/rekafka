package au.com.williamhill.rekafka;

import java.io.*;
import java.util.*;

import org.slf4j.*;

import com.beust.jcommander.*;
import com.obsidiandynamics.yconf.*;

import kafka.consumer.*;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.javaapi.producer.Producer;
import kafka.message.*;
import kafka.producer.*;

public final class Relay implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(Relay.class);
  
  private final PipeConfig config;
  
  private final long maxRecords;
  
  public Relay(PipeConfig config, long maxRecords) {
    this.config = config;
    this.maxRecords = maxRecords;
  }
  
  @Override
  public void run() {
    long relayed = 0;
    
    LOG.info("Starting relay");
    final ConsumerConnector consumer = Consumer.createJavaConsumerConnector(createConsumerConfig());
    final KafkaStream<byte[], byte[]> stream = createConsumerStream(consumer);
    
    final Producer<byte[], byte[]> producer = new Producer<>(createProducerConfig());
    final ConsumerIterator<byte[], byte[]> it = stream.iterator();
    while (it.hasNext()) {
      final MessageAndMetadata<byte[], byte[]> rx = it.next();
      relayed++;
      if (LOG.isTraceEnabled()) LOG.trace("Relaying {}/{}: key={}, value={}",
                                          relayed,
                                          maxRecords != 0 ? maxRecords : "\u221E",
                                          new String(rx.key()),
                                          new String(rx.message()));
      final KeyedMessage<byte[], byte[]> tx = new KeyedMessage<>(config.sink.topic, rx.key(), rx.message());
      producer.send(tx);
      
      if (maxRecords != 0 && relayed >= maxRecords) {
        LOG.info("Shutting down");
        break;
      }
    }

    producer.close();
    consumer.shutdown();
  }
  
  private KafkaStream<byte[], byte[]> createConsumerStream(ConsumerConnector consumer) {
    final Map<String, Integer> topicCountMap = new HashMap<>();
    topicCountMap.put(config.source.topic, 1);
    final Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer.createMessageStreams(topicCountMap);
    final List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(config.source.topic);
    if (streams.size() != 1) throw new IllegalStateException("Expected 1 stream; got " + streams.size());
    final KafkaStream<byte[], byte[]> stream = streams.get(0);
    return stream;
  }
  
  private ConsumerConfig createConsumerConfig() {
    return new ConsumerConfig(config.source.properties.build());
  }
  
  private ProducerConfig createProducerConfig() {
    return new ProducerConfig(config.sink.properties.build());
  }
  
  public static final class CommandArgs {
    @Parameter(names={"-c", "--conf"}, required=true, arity=1, description="Configuration file to load")
    String filename;
    
    @Parameter(names={"-m", "--max-records"}, required=false, arity=1, description="Max number of records to relay (defaults to unlimited)")
    Long maxRecords;
    
    @Parameter(names={"-h", "--help"}, help=true, description="Print this help screen")
    boolean help;
    
    boolean validate() {
      return filename != null && (maxRecords == null || maxRecords > 0);
    }
  }
  
  public static void main(String[] args) {
    final CommandArgs cmdArgs = new CommandArgs();
    final JCommander commander = JCommander.newBuilder()
    .addObject(cmdArgs)
    .build();
    
    try {
      commander.parse(args);
    } catch (Throwable e) {
      printUsageAndExit(commander, 1);
      return;
    }
    
    if (cmdArgs.help || ! cmdArgs.validate()) {
      printUsageAndExit(commander, cmdArgs.help ? 0 : 1);
      return;
    }
    
    LOG.info("Loading configuration {}", cmdArgs.filename);
    
    final PipeConfig config;
    try {
      config = new MappingContext()
          .withDomTransform(new JuelTransform())
          .withParser(new SnakeyamlParser())
          .fromReader(new FileReader(cmdArgs.filename))
          .map(PipeConfig.class);
    } catch (IOException e) {
      System.err.println("Error parsing configuration");
      e.printStackTrace();
      System.exit(1);
      return;
    }
    
    final long maxRecords = cmdArgs.maxRecords != null ? cmdArgs.maxRecords : 0;
    new Relay(config, maxRecords).run();
  }
  
  private static void printUsageAndExit(JCommander commander, int exitCode) {
    commander.usage();
    System.exit(exitCode);
  }
}
