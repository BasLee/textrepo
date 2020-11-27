package nl.knaw.huc.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class FormTextRepoFile {

  @NotNull(message = "is mandatory")
  private UUID docId;

  @NotNull(message = "is mandatory")
  private Short typeId;

  @JsonCreator
  public FormTextRepoFile(
      @JsonProperty("docId") UUID docId,
      @JsonProperty("typeId") Short typeId
  ) {
    this.docId = docId;
    this.typeId = typeId;
  }

  public UUID getDocId() {
    return docId;
  }

  public short getTypeId() {
    return typeId;
  }

  @Override
  public String toString() {
    return MoreObjects
        .toStringHelper(this)
        .add("docId", docId)
        .add("typeId", typeId)
        .toString();
  }
}
