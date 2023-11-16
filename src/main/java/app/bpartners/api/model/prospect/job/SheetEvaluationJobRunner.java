package app.bpartners.api.model.prospect.job;

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
public class SheetEvaluationJobRunner {
  public static final String GOLDEN_SOURCE_SPR_SHEET_NAME =
      "Golden source Depa1 Depa 2 - Prospect métier Antinuisibles  Serrurier ";

  private String artisanOwner;
  private EvaluationRules evaluationRules;
  private SheetProperties sheetProperties;
  private RatingProperties ratingProperties;
}
