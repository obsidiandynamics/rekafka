package au.com.williamhill.rekafka;

import java.util.*;

import org.slf4j.*;

import com.beust.jcommander.*;

import kafka.consumer.*;
import kafka.javaapi.consumer.ConsumerConnector;

public final class Relay implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(Relay.class);
  
  private final RelayConfig config;
  
  public Relay(RelayConfig config) {
    this.config = config;
  }
  
  @Override
  public void run() {
    final ConsumerConnector consumer = Consumer.createJavaConsumerConnector(createConsumerConfig());
    final Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
    topicCountMap.put(config.sourceConfig.topic, 1);
    final Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer.createMessageStreams(topicCountMap);
    final List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(config.sourceConfig.topic);
    if (streams.size() != 1) throw new IllegalStateException("Expected 1 stream; got " + streams.size());
    final KafkaStream<byte[], byte[]> stream = streams.get(0);
    
    final ConsumerIterator<byte[], byte[]> it = stream.iterator();
    while (it.hasNext()) {
      System.out.println(new String(it.next().message()));
    }
  }
  
  private ConsumerConfig createConsumerConfig() {
    return new ConsumerConfig(config.sourceConfig.props.build());
  }
  
  public static final class CommandArgs {
    @Parameter(names={"-c", "--conf"}, required=true, description="Configuration to load")
    String filename;
  }
  
  public static void main(String[] args) {
    args = new String[] {"-c", "file.yaml"}; //TODO
    
    final CommandArgs cmdArgs = new CommandArgs();
    final JCommander commander = JCommander.newBuilder()
    .addObject(cmdArgs)
    .build();
    
    try {
      commander.parse(args);
    } catch (Throwable e) {
      printUsageAndExit(commander);
      return;
    }
    LOG.info("Loading configuration {}", cmdArgs.filename);
    
    if (cmdArgs.filename == null) {
      printUsageAndExit(commander);
      return;
    }
    
    final RelayConfig config = new RelayConfig() {{
      sourceConfig = new EndConfig() {{
        topic = "platform.push";
        props = new PropertiesBuilder()
            .with("zookeeper.connect", "sitapzoo:2181")
            .with("group.id", "relay")
            .with("auto.commit.enable", false)
            .with("auto.offset.reset", "smallest");
      }};
      sinkConfig = new EndConfig() {{
        topic = "platform.push";
        props = new PropertiesBuilder()
            .with("zookeeper.connect", "sitapzoo:2181");
      }};
    }};
    
    new Relay(config).run();
  }
  
  private static void printUsageAndExit(JCommander commander) {
    commander.usage();
    System.exit(1);
  }
}
