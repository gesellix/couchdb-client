package de.gesellix.couchdb;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import com.squareup.moshi.Moshi;
import okhttp3.internal.Util;
import okio.Okio;

public class JsonMoshi implements Json {

  Moshi moshi;

  public JsonMoshi() {
    this(new Moshi.Builder());
  }

  public JsonMoshi(Moshi.Builder builder) {
    this.moshi = builder.build();
  }

  @Override
  public String encodeQueryValue(String key) {
    return moshi.adapter(String.class).toJson(key);
  }

  @Override
  public String encodeQueryValue(Collection<String> keys) {
    return moshi.adapter(Collection.class).toJson(keys);
  }

  @Override
  public String encodeDocument(Map<Object, Object> document) {
    return moshi.adapter(Map.class).toJson(document);
  }

  @Override
  public <T> T decodeDocument(String json, Class<T> type) throws IOException {
    return moshi.adapter(type).fromJson(json);
  }

  @Override
  public <T> T decodeDocument(String json, Type type) throws IOException {
    return (T) moshi.adapter(type).fromJson(json);
  }

  @Override
  public <T> T consume(InputStream stream, Class<T> type) throws IOException {
    T result = moshi.adapter(type).fromJson(Okio.buffer(Okio.source(stream)));
    Util.closeQuietly(stream);
    return result;
  }

  @Override
  public <T> T consume(InputStream stream, Type type) throws IOException {
//    Type type = Types.newParameterizedType(List.class, String.class);
    T result = (T) moshi.adapter(type).fromJson(Okio.buffer(Okio.source(stream)));
    Util.closeQuietly(stream);
    return result;
  }
}
