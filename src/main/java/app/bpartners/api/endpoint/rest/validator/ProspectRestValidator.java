package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.ImportProspect;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationRules;
import app.bpartners.api.endpoint.rest.model.RatingProperties;
import app.bpartners.api.endpoint.rest.model.SheetProperties;
import app.bpartners.api.endpoint.rest.model.SheetProspectEvaluation;
import app.bpartners.api.endpoint.rest.model.SheetRange;
import app.bpartners.api.endpoint.rest.model.UpdateProspect;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import java.util.function.Consumer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class ProspectRestValidator implements Consumer<UpdateProspect> {
  public static final String XLS_FILE = "application/vnd.ms-excel";
  public static final String XLSX_FILE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  @Override
  public void accept(UpdateProspect prospect) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (prospect.getId() == null) {
      exceptionMessageBuilder.append("Id is mandatory. ");
    }
    if (prospect.getStatus() == null) {
      exceptionMessageBuilder.append("Status is mandatory. ");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  public void accept(SheetProspectEvaluation evaluation) {
    StringBuilder builder = new StringBuilder();
    if (evaluation == null) {
      builder.append("Request body SheetProspectEvaluation is mandatory");
    } else {
      ProspectEvaluationRules evaluationRules = evaluation.getEvaluationRules();
      RatingProperties ratingProperties = evaluation.getRatingProperties();
      SheetProperties sheetProperties = evaluation.getSheetProperties();
      if (evaluation.getArtisanOwner() == null) {
        builder.append("ArtisanOwner is mandatory. ");
      }
      if (ratingProperties != null) {
        if (ratingProperties.getMinCustomerRating() == null) {
          ratingProperties.setMinCustomerRating(8.0);
        } else if (ratingProperties.getMinCustomerRating() < 0
            || ratingProperties.getMinCustomerRating() > 10) {
          builder.append(
              "Min customer rating must be between 0 and 10 but was "
                  + ratingProperties.getMinCustomerRating());
        }
        if (ratingProperties.getMinProspectRating() == null) {
          ratingProperties.setMinProspectRating(8.0);
        } else if (ratingProperties.getMinProspectRating() < 0
            || ratingProperties.getMinCustomerRating() > 10) {
          builder.append(
              "Min prospect rating must be between 0 and 10 but was "
                  + ratingProperties.getMinProspectRating());
        }
      } else {
        evaluation.setRatingProperties(
            new RatingProperties().minCustomerRating(8.0).minProspectRating(8.0));
      }
      if (evaluationRules == null) {
        builder.append("EvaluationRules is mandatory. ");
      } else {
        if (evaluationRules.getNewInterventionOption() == null) {
          builder.append("EvaluationRules.newInterventionOption is mandatory. ");
        }
      }
      if (sheetProperties == null) {
        builder.append("SheetProperties is mandatory. ");
      } else {
        if (sheetProperties.getSheetName() == null) {
          builder.append("SheetProperties.sheetName is mandatory. ");
        }
        if (sheetProperties.getSpreadsheetName() == null) {
          builder.append("SheetProperties.spreadsheetName is mandatory. ");
        }
        SheetRange ranges = sheetProperties.getRanges();
        if (ranges == null) {
          builder.append("SheetProperties.ranges is mandatory. ");
        } else {
          if (ranges.getMin() == null) {
            builder.append("SheetProperties.ranges.min is mandatory. ");
          }
          if (ranges.getMax() == null) {
            builder.append("SheetProperties.ranges.max is mandatory. ");
          }
        }
      }
    }
    String exceptionMessage = builder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  public void accept(ImportProspect importProspect) {
    StringBuilder messageBuilder = new StringBuilder();
    if (importProspect == null) {
      messageBuilder.append("ImportProspect is mandatory. ");
    } else {
      SheetProperties sheetProperties = importProspect.getSpreadsheetImport();
      if (sheetProperties == null) {
        messageBuilder.append("ImportProspect.sheetProperties is mandatory. ");
      } else {
        if (sheetProperties.getSpreadsheetName() == null) {
          messageBuilder.append("ImportProspect.sheetProperties.spreadsheetName is mandatory. ");
        }
        if (sheetProperties.getSheetName() == null) {
          messageBuilder.append("ImportProspect.sheetProperties.sheetName is mandatory. ");
        }
        if (sheetProperties.getRanges() == null) {
          messageBuilder.append("ImportProspect.sheetProperties.ranges is mandatory. ");
        }
      }
    }
    String exceptionMessage = messageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  public String validateAccept(String headerValue) {
    if (headerValue == null) {
      throw new BadRequestException(
          "Accept Header is mandatory. Types [application/json,"
              + XLS_FILE
              + ","
              + XLSX_FILE
              + "] are supported");
    }
    boolean isExcelFile = headerValue.equals(XLS_FILE) || headerValue.equals(XLSX_FILE);
    boolean isJsonFile = headerValue.equals(MediaType.APPLICATION_JSON_VALUE);
    if (!isExcelFile && !isJsonFile) {
      throw new NotImplementedException(
          headerValue
              + " is not supported."
              + "Only types [application/json,"
              + XLS_FILE
              + ","
              + XLSX_FILE
              + "] are supported");
    }
    return headerValue;
  }
}
