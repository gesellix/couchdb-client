package de.gesellix.couchdb;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import java.util.Map;

public class RowWithAuthorAdapter {

  @ToJson
  public Map<String, Object> toJson(RowWithAuthor from) {
    return from;
  }

  @FromJson
  public RowWithAuthor fromJson(Map<String, Object> delegate) {
    return new RowWithAuthor(delegate);
  }
}
