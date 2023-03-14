package app.bpartners.api.unit.repository;

import app.bpartners.api.model.AnnualRevenueTarget;
import app.bpartners.api.repository.implementation.ProspectRepositoryImpl;
import app.bpartners.api.service.AnnualRevenueTargetService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProspectRepositoryImplTest {

  @BeforeEach
  void setUp() {
    ProspectRepositoryImpl prospectRepositoryMock = mock(ProspectRepositoryImpl.class);
    AnnualRevenueTargetService revenueTargetServiceMock = mock(AnnualRevenueTargetService.class);

    when(revenueTargetServiceMock.getByYear(any() , any())).thenReturn(
        Optional.ofNullable(AnnualRevenueTarget.builder().build()));
  }

  @Test
  void needsProspects_ok(){

  }

}
