package app.bpartners.api.unit.service;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.CompanyInfo;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.service.AccountHolderService;
import app.bpartners.api.service.AnnualRevenueTargetService;
import app.bpartners.api.service.BusinessActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.utils.TestUtils.ACCOUNTHOLDER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        subject = new AccountHolderService(repositoryMock, buisnessActivityServiceMock, annualRevenueTargetServiceMock);
    }

    @Test
    void update_comany_info_ok_with_company_info_null() {
        var accountHolder = new AccountHolder();

        when(repositoryMock.findById(any())).thenReturn(accountHolder);
        when(repositoryMock.save(any())).thenReturn(accountHolder);

        assertEquals(accountHolder, subject.updateCompanyInfo(ACCOUNTHOLDER_ID, null));
    }
}
