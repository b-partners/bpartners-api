package app.bpartners.api.service.prospect.relaunch;

import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class HoldersProspectRelaunchService {
  private final ProspectToRelaunchFinder prospectToRelaunchFinder;
  private final AccountHolderJpaRepository accountHolderJpaRepository;
  private final ProspectRelaunchMailer mailer;

  public void relaunchHoldersProspects() {
    List<String> failures = new ArrayList<>();
    prospectToRelaunchFinder
        .findGroupedByHolder()
        .forEach((idHolder, prospects) -> relaunch(idHolder, prospects, failures));
    if (!failures.isEmpty()) {
      log.warn("Prospect relaunch failed for {} accountHolder(s): {}", failures.size(), failures);
    }
  }

  private void relaunch(String idHolder, List<Prospect> prospects, List<String> failures) {
    Optional<HAccountHolder> accountHolder = accountHolderJpaRepository.findById(idHolder);
    if (accountHolder.isEmpty()) {
      failures.add("AccountHolder(id=" + idHolder + ") was not found");
      return;
    }
    try {
      mailer.send(accountHolder.get(), prospects);
    } catch (RuntimeException e) {
      failures.add("AccountHolder(id=" + idHolder + ") was not relaunched: " + e.getMessage());
    }
  }
}
