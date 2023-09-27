package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.AntiHarmRules;
import app.bpartners.api.endpoint.rest.model.EventDateRanges;
import app.bpartners.api.endpoint.rest.model.EventEvaluationRules;
import app.bpartners.api.endpoint.rest.model.PutEventProspectConversion;
import app.bpartners.api.endpoint.rest.model.PutProspectEvaluationJob;
import app.bpartners.api.endpoint.rest.model.RatingProperties;
import app.bpartners.api.endpoint.rest.model.SheetProperties;
import app.bpartners.api.endpoint.rest.model.SheetRange;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.prospect.job.EvaluationRules;
import app.bpartners.api.model.prospect.job.EventJobRunner;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJobRunner;
import app.bpartners.api.model.prospect.job.SheetEvaluationJobRunner;
import org.springframework.stereotype.Component;

@Component
public class ProspectJobRestMapper {
  public ProspectEvaluationJobRunner toDomain(String ahId,
                                              PutProspectEvaluationJob rest) {
    PutEventProspectConversion eventConversion = rest.getEventProspectConversion();
    if (eventConversion != null) {
      var evaluationRules = eventConversion.getEvaluationRules();
      var ratingProperties = eventConversion.getRatingProperties();
      var sheetProperties = eventConversion.getSheetProperties();
      SheetRange sheetRange = sheetProperties.getRanges();
      var eventDateRanges = eventConversion.getEventDateRanges();
      var antiHarmRules = evaluationRules.getAntiHarmRules();
      return ProspectEvaluationJobRunner.builder()
          .jobId(rest.getJobId())
          .eventJobRunner(
              toDomain(ahId,
                  eventConversion.getCalendarId(),
                  evaluationRules,
                  ratingProperties,
                  sheetProperties,
                  sheetRange,
                  eventDateRanges,
                  antiHarmRules))
          .build();
    }
    throw new NotImplementedException(
        "Only PutEventProspectConversion is supported for no");
  }

  private EventJobRunner toDomain(String idAccountHolder,
                                  String calendarId,
                                  EventEvaluationRules evaluationRules,
                                  RatingProperties ratingProperties,
                                  SheetProperties sheetProperties,
                                  SheetRange sheetRange,
                                  EventDateRanges eventDateRanges,
                                  AntiHarmRules antiHarmRules) {
    return EventJobRunner.builder()
        .calendarId(calendarId)
        .sheetProspectEvaluation(
            toDomain(idAccountHolder,
                evaluationRules,
                ratingProperties,
                sheetProperties,
                sheetRange,
                antiHarmRules))
        .eventDateRanges(toDomain(eventDateRanges))
        .build();
  }

  private SheetEvaluationJobRunner toDomain(String idAccountHolder,
                                            EventEvaluationRules evaluationRules,
                                            RatingProperties ratingProperties,
                                            SheetProperties sheetProperties, SheetRange sheetRange,
                                            AntiHarmRules antiHarmRules) {
    return SheetEvaluationJobRunner.builder()
        .artisanOwner(idAccountHolder)
        .evaluationRules(toDomain(evaluationRules, antiHarmRules))
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
        .spreadsheetName(sheetProperties == null ? null
            : sheetProperties.getSpreadsheetName())
        .sheetName(sheetProperties == null ? null
            : sheetProperties.getSheetName())
        .ranges(app.bpartners.api.model.prospect.job.SheetRange.builder()
            .min(sheetRange == null ? null : sheetRange.getMin())
            .max(sheetRange == null ? null : sheetRange.getMax())
            .build())
        .build();
  }

  private app.bpartners.api.model.prospect.job.RatingProperties toDomain(
      RatingProperties ratingProperties) {
    return app.bpartners.api.model.prospect.job.RatingProperties.builder()
        .minCustomerRating(ratingProperties == null ? null
            : ratingProperties.getMinCustomerRating())
        .minProspectRating(ratingProperties == null ? null
            : ratingProperties.getMinProspectRating())
        .build();
  }

  private EvaluationRules toDomain(EventEvaluationRules evaluationRules,
                                   AntiHarmRules antiHarmRules) {
    return EvaluationRules.builder()
        .profession(evaluationRules == null ? null : evaluationRules.getProfession())
        .antiHarmRules(toDomain(antiHarmRules))
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
