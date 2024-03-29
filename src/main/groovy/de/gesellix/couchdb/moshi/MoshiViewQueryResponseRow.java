package de.gesellix.couchdb.moshi;

import com.squareup.moshi.Json;
import de.gesellix.couchdb.model.DocumentId;
import de.gesellix.couchdb.model.NonReducedViewQueryResponseRow;
import de.gesellix.couchdb.model.RowReference;

import java.util.Objects;

public class MoshiViewQueryResponseRow<KeyType, ValueType, DocType extends DocumentId>
    implements NonReducedViewQueryResponseRow<KeyType, ValueType, DocType>, RowReference<KeyType> {

  @Json(name = "id")
  public String id;

  @Json(name = "key")
  public KeyType key;

  @Json(name = "value")
  public ValueType value;

  /**
   * available when include_docs == true
   */
  @Json(name = "doc")
  public DocType doc;

  @Override
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getDocId() {
    return id;
  }

  @Override
  public KeyType getKey() {
    return key;
  }

  public void setKey(KeyType key) {
    this.key = key;
  }

  @Override
  public ValueType getValue() {
    return value;
  }

  public void setValue(ValueType value) {
    this.value = value;
  }

  @Override
  public DocType getDoc() {
    return doc;
  }

  public void setDoc(DocType doc) {
    this.doc = doc;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MoshiViewQueryResponseRow<?, ?, ?> that = (MoshiViewQueryResponseRow<?, ?, ?>) o;
    return Objects.equals(id, that.id) && Objects.equals(key, that.key);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, key);
  }

  @Override
  public String toString() {
    return "MoshiViewQueryResponseRow{" +
        "id='" + id + '\'' +
        ", key=" + key +
        ", value=" + value +
        ", doc=" + doc +
        '}';
  }
}
