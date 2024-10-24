package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.endpoint.rest.model.FileType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
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
public class FileSaved extends PojaEvent {
  private static final long serialVersionUID = 1L;

  @JsonProperty("fileType")
  private FileType fileType = null;

  @JsonProperty("accountId")
  private String accountId = null;

  @JsonProperty("fileId")
  private String fileId = null;

  @JsonProperty("fileAsBytes")
  private byte[] fileAsBytes = null;

  @JsonProperty("userId")
  private String userId = null;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FileSaved fileSaved = (FileSaved) o;
    return Objects.equals(this.fileType, fileSaved.fileType)
        && Objects.equals(this.accountId, fileSaved.accountId)
        && Objects.equals(this.fileId, fileSaved.fileId)
        && Objects.equals(this.userId, fileSaved.userId)
        && fileAsBytes.equals(fileSaved.fileAsBytes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileId, fileType, fileAsBytes, accountId);
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(3);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
