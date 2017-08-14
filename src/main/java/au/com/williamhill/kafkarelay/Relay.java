package au.com.williamhill.kafkarelay;

import org.slf4j.*;

import com.beust.jcommander.*;

public final class Relay {
  private static final Logger LOG = LoggerFactory.getLogger(Relay.class);
  
  public Relay(RelayConfig config) {
    
  }
  
  public static final class CLIOptions {
    @Parameter(names={"-c", "--conf"}, required=true, description="Configuration to load")
    String filename;
  }
  
  public static void main(String[] args) {
    args = new String[] {"-c", "file.yaml"}; //TODO
    
    final CLIOptions opts = new CLIOptions();
    final JCommander cmd = JCommander.newBuilder()
    .addObject(opts)
    .build();
    
    try {
      cmd.parse(args);
    } catch (Throwable e) {
      cmd.usage();
      System.exit(1);;
    }
    LOG.info("Loading configuration {}", opts.filename);
    
    final RelayConfig config = new RelayConfig() {{
      sourceConfig = new EndConfig() {{
        topic = "platform.push";
        props = new PropertiesBuilder()
            .with("group.id", "flywheel-relay")
            .with("auto.commit.offset", false);
      }};
    }};
  }
}
