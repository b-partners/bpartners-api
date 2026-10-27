package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotation;
import java.time.Duration;
import lombok.*;

@Builder
@ToString
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExportAreaPictureAnnotationRequested extends PojaEvent {
  private String userId;

  @SuppressWarnings("java:S1948") // event is JSON-serialized, not Java-serialized
  private ExportAreaPictureAnnotation annotation;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(5);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
