package app.bpartners.api.unit.service;

import static app.bpartners.api.integration.conf.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.CompanyInfo;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.service.AccountHolderService;
import app.bpartners.api.service.AnnualRevenueTargetService;
import app.bpartners.api.service.BusinessActivityService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountHolderServiceTest {
  AccountHolderService subject;
  AccountHolderRepository repositoryMock;
  BusinessActivityService buisnessActivityServiceMock;
  AnnualRevenueTargetService annualRevenueTargetServiceMock;

  @BeforeEach
  void setUp() {
    repositoryMock = mock(AccountHolderRepository.class);
    buisnessActivityServiceMock = mock(BusinessActivityService.class);
    annualRevenueTargetServiceMock = mock(AnnualRevenueTargetService.class);
    subject =
        new AccountHolderService(
            repositoryMock, buisnessActivityServiceMock, annualRevenueTargetServiceMock);
  }

  @Test
  void get_by_id_ok() {
    var accountHolder = mock(AccountHolder.class);
    when(repositoryMock.findById(any())).thenReturn(accountHolder);

    assertEquals(accountHolder, subject.getById(JOE_DOE_ACCOUNT_HOLDER_ID));
  }

  @Test
  void update_company_info_ok_with_company_info_null() {
    var accountHolder = new AccountHolder();
    when(repositoryMock.findById(any())).thenReturn(accountHolder);
    when(repositoryMock.save(any())).thenReturn(accountHolder);

    assertEquals(accountHolder, subject.updateCompanyInfo(ACCOUNTHOLDER_ID, null));
  }

  CompanyInfo companyInfo() {
    return CompanyInfo.builder()
        .socialCapital(4000)
        .email("email@example.com")
        .website("website example")
        .phone("+33 12 34 56 78")
        .subjectToVat(true)
        .location(location())
        .townCode(92002)
        .tvaNumber("FR12323456789")
        .build();
  }

  @Test
  void update_company_info_ok_with_company_info_not_null() {
    var accountHolder =
        AccountHolder.builder()
            .socialCapital(4000)
            .email("email@example.com")
            .website("website example")
            .mobilePhoneNumber("+33 12 34 56 78")
            .subjectToVat(true)
            .location(location())
            .townCode(92002)
            .vatNumber("FR12323456789")
            .build();
    when(repositoryMock.findById(any())).thenReturn(accountHolder);
    when(repositoryMock.save(any())).thenReturn(accountHolder);

    assertEquals(accountHolder, subject.updateCompanyInfo(ACCOUNTHOLDER_ID, companyInfo()));
  }

  @Test
  void find_default_by_id_user_return_null() {
    when(repositoryMock.findAllByUserId(any())).thenReturn(List.of());

    assertEquals(null, subject.findDefaultByIdUser(USER1_ID));
  }

  @Test
  void find_default_by_id_user_account_holders_ok() {
    var defaultAccountHolder = mock(AccountHolder.class);
    var accountHolder = mock(AccountHolder.class);
    when(repositoryMock.findAllByUserId(any()))
        .thenReturn(List.of(defaultAccountHolder, accountHolder));

    assertEquals(defaultAccountHolder, subject.findDefaultByIdUser(USER1_ID));
  }
}
