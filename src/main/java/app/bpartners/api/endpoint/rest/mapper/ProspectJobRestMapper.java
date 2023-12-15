package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.AntiHarmRules;
import app.bpartners.api.endpoint.rest.model.EventDateRanges;
import app.bpartners.api.endpoint.rest.model.EventEvaluationRules;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationRules;
import app.bpartners.api.endpoint.rest.model.PutEventProspectConversion;
import app.bpartners.api.endpoint.rest.model.PutProspectEvaluationJob;
import app.bpartners.api.endpoint.rest.model.RatingProperties;
import app.bpartners.api.endpoint.rest.model.SheetProperties;
import app.bpartners.api.endpoint.rest.model.SheetProspectEvaluation;
import app.bpartners.api.endpoint.rest.model.SheetRange;
import app.bpartners.api.endpoint.rest.validator.ProspectJobValidator;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.prospect.job.EvaluationRules;
import app.bpartners.api.model.prospect.job.EventJobRunner;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJobRunner;
import app.bpartners.api.model.prospect.job.SheetEvaluationJobRunner;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProspectJobRestMapper {
  private final ProspectJobValidator jobValidator;

  @SneakyThrows
  public ProspectEvaluationJobRunner toDomain(String ahId, PutProspectEvaluationJob rest) {
    jobValidator.accept(rest);
    PutEventProspectConversion eventConversion = rest.getEventProspectConversion();
    SheetProspectEvaluation spreadSheetEvaluation = rest.getSpreadSheetEvaluation();
    if (eventConversion != null && spreadSheetEvaluation != null) {
      throw new NotImplementedException(
          "Only eventProspectConversion or spreadSheetEvaluation can be running"
              + " at once for now but no both");
    }
    Map<String, String> metadata = rest.getMetadata() == null ? Map.of() : rest.getMetadata();
    if (eventConversion != null) {
      var evaluationRules = eventConversion.getEvaluationRules();
      var ratingProperties = eventConversion.getRatingProperties();
      var eventDateRanges = eventConversion.getEventDateRanges();
      var antiHarmRules = evaluationRules.getAntiHarmRules();
      return ProspectEvaluationJobRunner.builder()
          .jobId(rest.getJobId())
          .metadata(metadata)
          .eventJobRunner(
              toDomain(
                  ahId,
                  eventConversion.getCalendarId(),
                  evaluationRules,
                  ratingProperties,
                  eventDateRanges,
                  antiHarmRules))
          .build();
    } else if (spreadSheetEvaluation != null) {
      ProspectEvaluationRules evaluationRules = spreadSheetEvaluation.getEvaluationRules();
      RatingProperties ratingProperties = spreadSheetEvaluation.getRatingProperties();
      SheetProperties sheetProperties = spreadSheetEvaluation.getSheetProperties();
      SheetRange sheetRange = sheetProperties.getRanges();
      return ProspectEvaluationJobRunner.builder()
          .jobId(rest.getJobId())
          .metadata(metadata)
          .sheetJobRunner(
              toDomain(
                  spreadSheetEvaluation.getArtisanOwner(),
                  evaluationRules,
                  ratingProperties,
                  sheetProperties,
                  sheetRange))
          .build();
    }
    throw new NotImplementedException(
        "Only PutEventProspectConversion and SpreadSheetEvaluation is supported for now");
  }

  private EventJobRunner toDomain(
      String idAccountHolder,
      String calendarId,
      EventEvaluationRules evaluationRules,
      RatingProperties ratingProperties,
      EventDateRanges eventDateRanges,
      AntiHarmRules antiHarmRules) {
    return EventJobRunner.builder()
        .calendarId(calendarId)
        .artisanOwner(idAccountHolder)
        .evaluationRules(toDomain(evaluationRules, antiHarmRules))
        .ratingProperties(toDomain(ratingProperties))
        .eventDateRanges(toDomain(eventDateRanges))
        .build();
  }

  private SheetEvaluationJobRunner toDomain(
      String idAccountHolder,
      ProspectEvaluationRules evaluationRules,
      RatingProperties ratingProperties,
      SheetProperties sheetProperties,
      SheetRange sheetRange) {
    return SheetEvaluationJobRunner.builder()
        .artisanOwner(idAccountHolder)
        .evaluationRules(toDomain(evaluationRules))
        .ratingProperties(toDomain(ratingProperties))
        .sheetProperties(toDomain(sheetProperties, sheetRange))
        .build();
  }

  private EventJobRunner.EventDateRanges toDomain(EventDateRanges eventDateRanges) {
    return EventJobRunner.EventDateRanges.builder()
        .from(eventDateRanges.getFrom())
        .to(eventDateRanges.getTo())
        .build();
  }

  private app.bpartners.api.model.prospect.job.SheetProperties toDomain(
      SheetProperties sheetProperties, SheetRange sheetRange) {
    return app.bpartners.api.model.prospect.job.SheetProperties.builder()
        .spreadsheetName(sheetProperties == null ? null : sheetProperties.getSpreadsheetName())
        .sheetName(sheetProperties == null ? null : sheetProperties.getSheetName())
        .ranges(
            app.bpartners.api.model.prospect.job.SheetRange.builder()
                .min(sheetRange == null ? null : sheetRange.getMin())
                .max(sheetRange == null ? null : sheetRange.getMax())
                .build())
        .build();
  }

  private app.bpartners.api.model.prospect.job.RatingProperties toDomain(
      RatingProperties ratingProperties) {
    return app.bpartners.api.model.prospect.job.RatingProperties.builder()
        .minCustomerRating(
            ratingProperties == null ? null : ratingProperties.getMinCustomerRating())
        .minProspectRating(
            ratingProperties == null ? null : ratingProperties.getMinProspectRating())
        .build();
  }

  private EvaluationRules toDomain(
      EventEvaluationRules evaluationRules, AntiHarmRules antiHarmRules) {
    return EvaluationRules.builder()
        .profession(evaluationRules == null ? null : evaluationRules.getProfession())
        .antiHarmRules(toDomain(antiHarmRules))
        .interventionOption(null)
        .build();
  }

  private EvaluationRules toDomain(ProspectEvaluationRules evaluationRules) {
    return EvaluationRules.builder()
        .profession(evaluationRules == null ? null : evaluationRules.getProfession())
        .antiHarmRules(
            toDomain(evaluationRules == null ? null : evaluationRules.getAntiHarmRules()))
        .interventionOption(
            evaluationRules == null ? null : evaluationRules.getNewInterventionOption())
        .build();
  }

  private app.bpartners.api.model.prospect.job.AntiHarmRules toDomain(
      AntiHarmRules restAntiHarmRules) {
    return app.bpartners.api.model.prospect.job.AntiHarmRules.builder()
        .infestationType(restAntiHarmRules == null ? null : restAntiHarmRules.getInfestationType())
        .interventionTypes(
            restAntiHarmRules == null ? null : restAntiHarmRules.getInterventionTypes())
        .build();
  }
}
