package au.com.williamhill.kafkarelay;

import java.util.*;

public final class PropertiesBuilder {
  private final Properties properties = new Properties();
  
  public PropertiesBuilder with(String key, Object value) {
    if (value != null) properties.put(key, value.toString());
    return this;
  }
  
  public Properties build() {
    return properties;
  }
}
