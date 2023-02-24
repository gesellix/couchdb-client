package de.gesellix.couchdb.moshi;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;
import java.time.LocalDate;

// from https://github.com/square/moshi/blob/cc4b5b3ad2127e48937c2ca8b8fcb31b10ce4e0b/adapters/src/test/java/com/squareup/moshi/adapters/Rfc3339DateJsonAdapterTest.java
public class LocalDateJsonAdapter extends JsonAdapter<LocalDate> {

  @Override
  public synchronized LocalDate fromJson(JsonReader reader) throws IOException {
    if (reader.peek().equals(JsonReader.Token.NULL)) {
      return reader.nextNull();
    }

    String string = reader.nextString();
    return LocalDate.parse(string);
  }

  @Override
  public synchronized void toJson(JsonWriter writer, LocalDate value) throws IOException {
    if (value == null) {
      writer.nullValue();
    }
    else {
      String string = value.toString();
      writer.value(string);
    }
  }
}
