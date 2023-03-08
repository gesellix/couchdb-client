package de.gesellix.couchdb.moshi;

import com.squareup.moshi.Json;
import de.gesellix.couchdb.model.ReducedViewQueryResponse;
import de.gesellix.couchdb.model.RowReference;

import java.util.ArrayList;
import java.util.List;

public class MoshiReducedViewQueryResponse<KeyType, Row extends RowReference<KeyType>>
    implements ReducedViewQueryResponse<KeyType, Row> {

  @Json(name = "rows")
  private List<Row> rows = new ArrayList<>();

  @Override
  public List<Row> getRows() {
    return rows;
  }


  public void setRows(List<Row> rows) {
    this.rows = rows;
  }

  @Override
  public String toString() {
    return "MoshiReducedViewQueryResponse{" +
        "rows=" + rows +
        '}';
  }
}
