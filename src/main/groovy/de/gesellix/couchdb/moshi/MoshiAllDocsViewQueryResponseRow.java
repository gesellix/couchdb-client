package de.gesellix.couchdb.moshi;

import com.squareup.moshi.Json;
import de.gesellix.couchdb.model.DocumentId;
import de.gesellix.couchdb.model.RowReference;

import java.util.Objects;

public class MoshiAllDocsViewQueryResponseRow<DocType extends DocumentId> implements RowReference<String> {

  @Json(name = "id")
  public String id;

  @Json(name = "key")
  public String key;

  @Json(name = "value")
//  Map<String, Object> value;
  @NestedRevision
  public String rev;

  /**
   * available when include_docs == true
   */
  @Json(name = "doc")
  public DocType doc;

  @Override
  public String getDocId() {
    return id;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getRev() {
    return rev;
  }

  public void setRev(String rev) {
    this.rev = rev;
  }

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
    MoshiAllDocsViewQueryResponseRow<?> that = (MoshiAllDocsViewQueryResponseRow<?>) o;
    return Objects.equals(id, that.id) && Objects.equals(key, that.key) && Objects.equals(rev, that.rev);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, key, rev);
  }

  @Override
  public String toString() {
    return "ViewQueryResponseRow{" +
        "id='" + id + '\'' +
        ", rev='" + rev + '\'' +
        ", doc=" + doc +
        '}';
  }
}
