package de.gesellix.couchdb;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public interface Json {

  String encodeQueryValue(String key);

  String encodeQueryValue(Collection<String> keys);

  String encodeDocument(Map<Object, Object> document);

  <T> T decodeDocument(String json, Class<T> type) throws IOException;

  <T> T decodeDocument(String json, Type type) throws IOException;

  <T> T consume(InputStream stream, Class<T> type) throws IOException;

  <T> T consume(InputStream stream, Type type) throws IOException;
}
