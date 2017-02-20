package dataengine.sessions;

import static net.deelam.graph.GrafTxn.tryOn;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedTransactionalGraph;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public final class SessionDB_FrameHelper {

  private final FramedTransactionalGraph<TransactionalGraph> graph;

  public <T> T getVertexFrame(String qualId, Class<T> clazz) {
    return tryOn(graph, graph -> {
      T vf = graph.getVertex(qualId, clazz);
      if (vf == null)
        throw new IllegalArgumentException("Node does not exists with id=" + qualId);
      return vf;
    });
  }

  @SuppressWarnings("unchecked")
  public static void saveMapAsProperties(Map<?, ?> map, Vertex v, String propPrefix) {
    for (Map.Entry<String, Object> e : ((Map<String, Object>) map).entrySet()) {
      setVertexProperty(v, propPrefix, e.getKey(), e.getValue());
    }
  }

  static void setVertexProperty(Vertex v, String propPrefix, String keySuffix, Object val) {
    if (val instanceof String ||
        val instanceof Number ||
        val instanceof Boolean)
      v.setProperty(propPrefix + keySuffix, val);
    else {
      log.warn("Storing as string -- don't know how to store value as property {}: {} {}", propPrefix + keySuffix,
          val.getClass(), val);
      v.setProperty(propPrefix + keySuffix, val.toString());
    }
  }

  public static Map<String, Object> loadPropertiesAsMap(Vertex v, String propPrefix) {
    int ppIndex = propPrefix.length();
    Map<String, Object> map = new HashMap<>();
    for (String key : v.getPropertyKeys()) {
      if (key.startsWith(propPrefix)) {
        map.put(key.substring(ppIndex), v.getProperty(key));
      }
    }
    return map;
  }


  public static OffsetDateTime toOffsetDateTime(DateTime createdTime) {
    return OffsetDateTime.ofInstant(Instant.ofEpochMilli(createdTime.getMillis()), 
        ZoneOffset.UTC);
  }

  public static DateTime toJodaDateTime(OffsetDateTime offsetDateTime) {
    return new DateTime(offsetDateTime.toInstant().toEpochMilli());
  }

}