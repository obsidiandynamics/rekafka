package au.com.williamhill.rekafka;

import com.obsidiandynamics.yconf.*;

@Y
public class PipeConfig {
  @YInject
  ConnectorConfig source = new ConnectorConfig();
  
  @YInject
  ConnectorConfig sink = new ConnectorConfig();
}
