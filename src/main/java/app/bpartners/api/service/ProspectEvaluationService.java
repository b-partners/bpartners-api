package app.bpartners.api.service;

import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.expressif.ProspectEval;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProspectEvaluationService {
  public List<ProspectEval> saveAll(List<ProspectEval> prospectEvaluations) {
    throw new NotImplementedException("Not supported");
  }
}
