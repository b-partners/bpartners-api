package app.bpartners.api.service;

import app.bpartners.api.model.AnnualRevenueTarget;
import app.bpartners.api.repository.AnnualRevenueTargetRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AnnualRevenueTargetService {
  private final AnnualRevenueTargetRepository repository;

  public List<AnnualRevenueTarget> save(List<AnnualRevenueTarget> toCreate) {
    return repository.saveAll(toCreate);
  }

  public List<AnnualRevenueTarget> getAnnualRevenueTargets(String accountHolderId) {
    return repository.getAnnualRevenueTargets(accountHolderId);
  }

}
