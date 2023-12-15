package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.PutProspectEvaluationJob;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class ProspectJobValidator implements Consumer<PutProspectEvaluationJob> {
  @Override
  public void accept(PutProspectEvaluationJob putProspectEvaluationJob) {
    StringBuilder exceptionMsgBuilder = new StringBuilder();
    if (putProspectEvaluationJob == null) {
      exceptionMsgBuilder.append("PutProspectEvaluationJob is mandatory");
    } else {
      if (putProspectEvaluationJob.getJobId() == null) {
        exceptionMsgBuilder.append("JobId is mandatory. ");
      }
      if (putProspectEvaluationJob.getEventProspectConversion() == null
          && putProspectEvaluationJob.getSpreadSheetEvaluation() == null) {
        exceptionMsgBuilder.append(
            "Both EventProspectConversion and SpreadSheetEvaluation can not be null. ");
      }
      // TODO: add other EventProspectConversion and SpreadSheetEvaluation attributes validation
    }
    String exceptionMsg = exceptionMsgBuilder.toString();
    if (!exceptionMsg.isEmpty()) {
      throw new BadRequestException(exceptionMsg);
    }
  }
}
