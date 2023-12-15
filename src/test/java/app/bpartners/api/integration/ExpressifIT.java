package app.bpartners.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.repository.expressif.ExpressifApi;
import app.bpartners.api.repository.expressif.model.InputForm;
import app.bpartners.api.repository.expressif.model.InputValue;
import app.bpartners.api.repository.expressif.model.OutputValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@Slf4j
public class ExpressifIT extends MockedThirdParties {
  @Autowired private ExpressifApi subject;

  private List<OutputValue<Object>> expected(Instant evaluationDate) {
    return List.of(
        OutputValue.builder()
            .evaluationDate(evaluationDate)
            .name("Métier dépanneur")
            .value(true)
            .build(),
        OutputValue.builder()
            .evaluationDate(evaluationDate)
            .name("Typologie client")
            .value("correcte")
            .build(),
        OutputValue.builder()
            .evaluationDate(evaluationDate)
            .name("Notation de l'ancien client")
            .value(10.0)
            .build());
  }

  @Test
  void process_prospect_ok() throws JsonProcessingException {
    Instant evaluationDate = Instant.parse("2023-04-01T06:06:00.511Z");
    InputForm input =
        InputForm.builder()
            .evaluationDate(evaluationDate)
            .inputValues(
                List.of(
                    new InputValue<>(evaluationDate, "Antinuisibles 3D", false),
                    new InputValue<>(evaluationDate, "Serrurier", true),
                    new InputValue<>(evaluationDate, "Clientèle professionnelle", true),
                    new InputValue<>(evaluationDate, "Clientèle particulier", true),
                    new InputValue<>(evaluationDate, "Intervention prévue", true),
                    new InputValue<>(evaluationDate, "Le type de client", "particulier"),
                    new InputValue<>(
                        evaluationDate,
                        "La distance entre l'intervention prévue et l'ancien client",
                        200.0)))
            .build();

    List<OutputValue> actual = subject.process(input);

    assertEquals(expected(evaluationDate), actual);
  }
}
