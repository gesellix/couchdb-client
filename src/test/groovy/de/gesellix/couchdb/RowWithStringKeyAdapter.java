package de.gesellix.couchdb;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import java.util.Map;

public class RowWithStringKeyAdapter {

  @ToJson
  public Map<String, Object> toJson(RowWithStringKey from) {
    return from;
  }

  @FromJson
  public RowWithStringKey fromJson(Map<String, Object> delegate) {
    return new RowWithStringKey(delegate);
  }
}
