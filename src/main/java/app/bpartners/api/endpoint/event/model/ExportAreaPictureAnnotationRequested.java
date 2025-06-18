package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import java.time.Duration;
import lombok.*;

@Builder
@ToString
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExportAreaPictureAnnotationRequested extends PojaEvent {
  private String userId;
  private ExportAreaPictureAnnotation annotation;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(5);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return null;
  }
}
