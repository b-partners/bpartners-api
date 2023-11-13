package app.bpartners.api.service;

import java.util.List;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class InvoiceRelaunchSchedule {
  private final InvoiceRelaunchService relaunchService;

  @PostConstruct
  void relaunchInvoices() {
    relaunchService.restartLastRelaunch(
        List.of("29225a52-8b86-401e-b0a5-dcf6330a8d66",
            "7e12b680f-4777-48a5-8c04-f6214289f443"));
  }
}
