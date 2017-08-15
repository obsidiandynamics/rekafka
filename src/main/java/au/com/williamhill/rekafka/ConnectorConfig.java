package au.com.williamhill.rekafka;

import com.obsidiandynamics.yconf.*;

@Y
public class ConnectorConfig {
  @YInject
  public String topic;
  
  @YInject
  public PropertiesBuilder properties = new PropertiesBuilder();
}
