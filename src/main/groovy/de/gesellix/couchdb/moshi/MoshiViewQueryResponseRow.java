package de.gesellix.couchdb.moshi;

import com.squareup.moshi.Json;
import de.gesellix.couchdb.ViewQueryResponseRow;

public class MoshiViewQueryResponseRow<DocType> implements ViewQueryResponseRow<DocType> {

  @Json(name = "id")
  public String id;
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
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getRev() {
    return rev;
  }

  public void setRev(String rev) {
    this.rev = rev;
  }

  @Override
  public DocType getDoc() {
    return doc;
  }

  public void setDoc(DocType doc) {
    this.doc = doc;
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
