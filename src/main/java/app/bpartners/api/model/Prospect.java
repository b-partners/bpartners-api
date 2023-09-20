package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.ProspectFeedback;
import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class Prospect implements Comparable<Prospect> {
  private String id;
  private String idJob;
  private String idHolderOwner;
  private String name;
  private String email;
  private String phone;
  private Geojson location;
  private String address;
  private ProspectStatus status;
  private Integer townCode;
  private ProspectRating rating;
  private String comment;
  private Fraction contractAmount;
  private String idInvoice;
  private ProspectFeedback prospectFeedback;

  @Override
  public int compareTo(Prospect o) {
    ProspectRating actualRating = this.getRating();
    ProspectRating otherRating = o.getRating();
    if (actualRating == null || actualRating.getValue() == null) {
      return 0;
    } else if (otherRating == null || otherRating.getValue() == null) {
      return 0;
    }
    if (actualRating.getValue() < otherRating.getValue()) {
      return -1;
    } else if (actualRating.getValue() > otherRating.getValue()) {
      return 1;
    }
    return 0;
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @EqualsAndHashCode
  @ToString
  public static class ProspectRating {
    private Double value;
    private Instant lastEvaluationDate;
  }
}
