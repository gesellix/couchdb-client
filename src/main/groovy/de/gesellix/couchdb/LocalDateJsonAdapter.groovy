package de.gesellix.couchdb

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

import java.time.LocalDate

// from https://github.com/square/moshi/blob/cc4b5b3ad2127e48937c2ca8b8fcb31b10ce4e0b/adapters/src/test/java/com/squareup/moshi/adapters/Rfc3339DateJsonAdapterTest.java
class LocalDateJsonAdapter extends JsonAdapter<LocalDate> {

  @Override
  synchronized LocalDate fromJson(JsonReader reader) throws IOException {
    if (reader.peek() == JsonReader.Token.NULL) {
      return reader.nextNull()
    }
    String string = reader.nextString()
    return LocalDate.parse(string)
  }

  @Override
  synchronized void toJson(JsonWriter writer, LocalDate value) throws IOException {
    if (value == null) {
      writer.nullValue()
    }
    else {
      String string = value.toString()
      writer.value(string)
    }
  }
}
