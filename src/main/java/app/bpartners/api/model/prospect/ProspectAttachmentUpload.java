package app.bpartners.api.model.prospect;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProspectAttachmentUpload {
  String fileKey;
  String uploadUrl;
  Instant expiresAt;
}
