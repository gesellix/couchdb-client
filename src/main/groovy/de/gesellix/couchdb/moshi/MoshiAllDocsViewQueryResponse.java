package de.gesellix.couchdb.moshi;

import com.squareup.moshi.Json;
import de.gesellix.couchdb.model.DocumentId;
import de.gesellix.couchdb.model.NonReducedViewQueryResponse;

import java.util.ArrayList;
import java.util.List;

public class MoshiAllDocsViewQueryResponse<DocType extends DocumentId> implements NonReducedViewQueryResponse<String, MoshiAllDocsViewQueryResponseRow<DocType>> {

  @Json(name = "offset")
  private Integer offset;
  @Json(name = "total_rows")
  private Integer totalRows;
  @Json(name = "rows")
  private List<MoshiAllDocsViewQueryResponseRow<DocType>> rows = new ArrayList<>();
  @Json(name = "update_seq")
  private Object updateSeq;

  @Override
  public Integer getOffset() {
    return offset;
  }

  public void setOffset(Integer offset) {
    this.offset = offset;
  }

  @Override
  public Integer getTotalRows() {
    return totalRows;
  }

  public void setTotalRows(Integer totalRows) {
    this.totalRows = totalRows;
  }

  @Override
  public List<MoshiAllDocsViewQueryResponseRow<DocType>> getRows() {
    return rows;
  }


  public void setRows(List<MoshiAllDocsViewQueryResponseRow<DocType>> rows) {
    this.rows = rows;
  }

  public void setUpdateSeq(Object updateSeq) {
    this.updateSeq = updateSeq;
  }

  public Object getUpdateSeq() {
    return updateSeq;
  }

  @Override
  public String toString() {
    return "ViewQueryResponse{" +
        "offset=" + offset +
        ", totalRows=" + totalRows +
        ", rows=" + rows +
        ", updateSeq=" + updateSeq +
        '}';
  }
}
