package app.bpartners.api.model.prospect.job;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class EventJobRunner {
  private String calendarId;
  private String artisanOwner;
  private EventDateRanges eventDateRanges;
  private EvaluationRules evaluationRules;
  private RatingProperties ratingProperties;

  @Data
  @Builder(toBuilder = true)
  @AllArgsConstructor
  @NoArgsConstructor
  @EqualsAndHashCode
  @ToString
  public static class EventDateRanges {
    private Instant from;
    private Instant to;
  }
}
