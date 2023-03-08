package de.gesellix.couchdb.moshi;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;
import de.gesellix.couchdb.model.MapWithDocumentId;

import java.util.Map;

public class MapWithDocumentIdAdapter {

  @ToJson
  Map<String, Object> toJson(MapWithDocumentId<Object> from) {
    return from;
  }

  @FromJson
  MapWithDocumentId<Object> fromJson(Map<String, Object> delegate) {
    return new MapWithDocumentId<>(delegate);
  }
}
