package app.bpartners.api.endpoint.event.model.gen;

import app.bpartners.api.endpoint.rest.model.FileType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Objects;
import javax.annotation.processing.Generated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Generated("EventBridge")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@ToString
public class FileUploaded implements Serializable {
  private static final long serialVersionUID = 1L;

  @JsonProperty("fileType")
  private FileType fileType = null;
  @JsonProperty("accountId")
  private String accountId = null;
  @JsonProperty("fileId")
  private String fileId = null;
  @JsonProperty("fileAsBytes")
  private byte[] fileAsBytes = null;

  @JsonProperty("invoiceId")
  private String invoiceId = null;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FileUploaded fileUploaded = (FileUploaded) o;
    return Objects.equals(this.fileType, fileUploaded.fileType)
        && Objects.equals(this.accountId, fileUploaded.accountId)
        && Objects.equals(this.fileId, fileUploaded.fileId)
        && fileAsBytes.equals(fileUploaded.fileAsBytes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileId, fileType, fileAsBytes, accountId);
  }
}
