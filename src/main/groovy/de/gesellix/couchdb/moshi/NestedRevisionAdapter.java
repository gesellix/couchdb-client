package de.gesellix.couchdb.moshi;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import java.util.Map;

public class NestedRevisionAdapter {

  @ToJson
  Map<String, String> toJson(@NestedRevision String revision) {
    throw new UnsupportedOperationException("writing 'revision' currently not supported");
  }

  @FromJson
  @NestedRevision
  String fromJson(Map<String, Object> json) {
    Object rev = json.get("rev");
    return rev == null ? null : rev.toString();
  }

//  @FromJson
//  @NestedRevision
//  String fromJson(JsonReader json) throws IOException {
//    String result = null;
//
//    json.beginObject();
//    int nameAtIndex;
//    // skip everything unless it's the 'rev'
//    while (-1 == (nameAtIndex = json.selectName(JsonReader.Options.of("rev")))) {
//      json.nextSource();
//      if (json.peek() == JsonReader.Token.END_OBJECT) {
//        break;
//      }
//    }
//    // found the 'rev'?
//    if (nameAtIndex != -1) {
//      result = json.nextString();
//    }
//    // skip the rest
//    while (json.peek() != JsonReader.Token.END_OBJECT) {
//      json.promoteNameToValue();
//      json.nextString();
//      json.nextSource();
//    }
//    json.endObject();
//    return result;
//  }
}
