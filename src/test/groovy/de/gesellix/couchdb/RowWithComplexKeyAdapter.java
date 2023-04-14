package de.gesellix.couchdb;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import java.util.Map;

public class RowWithComplexKeyAdapter {

  @ToJson
  public Map<String, Object> toJson(RowWithComplexKey from) {
    return from;
  }

  @FromJson
  public RowWithComplexKey fromJson(Map<String, Object> delegate) {
    return new RowWithComplexKey(delegate);
  }
}
