package app.bpartners.api.unit.repository;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.MonthlyTransactionsSummary;
import app.bpartners.api.model.mapper.TransactionsSummaryMapper;
import app.bpartners.api.repository.implementation.TransactionsSummaryRepositoryImpl;
import app.bpartners.api.repository.jpa.TransactionsSummaryJpaRepository;
import app.bpartners.api.repository.jpa.model.HMonthlyTransactionsSummary;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransactionSummaryRepositoryTest {
  TransactionsSummaryRepositoryImpl summaryRepository;
  TransactionsSummaryJpaRepository jpaRepository;
  TransactionsSummaryMapper mapper;

  @BeforeEach
  void setUp() {
    jpaRepository = mock(TransactionsSummaryJpaRepository.class);
    mapper = new TransactionsSummaryMapper();
    summaryRepository = new TransactionsSummaryRepositoryImpl(jpaRepository, mapper);
    when(jpaRepository.save(any(HMonthlyTransactionsSummary.class))).thenAnswer(
        i -> {
          HMonthlyTransactionsSummary answer = i.getArgument(0);
          if (answer.getId() == null) {
            answer.setId("random_id");
          }
          return answer;
        }
    );
    when(jpaRepository.getByIdAccountAndYearAndMonth(
        eq(JOE_DOE_ACCOUNT_ID),
        eq(YearMonth.now().getYear()),
        any(Integer.class))
    ).thenReturn(persisted());
    when(jpaRepository.getByIdAccountAndYearAndMonth(
        eq(JOE_DOE_ACCOUNT_ID),
        eq(2021),
        any(Integer.class))
    ).thenReturn(null);
  }

  @Test
  void crupdate_yearmonth_summary_ok() {
    MonthlyTransactionsSummary actual = summaryRepository.updateYearMonthSummary(
        JOE_DOE_ACCOUNT_ID,
        YearMonth.now().getYear(),
        updated()
    );
    MonthlyTransactionsSummary actual2 = summaryRepository.updateYearMonthSummary(
        JOE_DOE_ACCOUNT_ID,
        2021,
        updated()
    );

    assertEquals(updated(), actual);
    assertEquals(updated(), actual2);
  }

  @Test
  void get_summaries_by_yearmonth() {
    YearMonth now = YearMonth.now();

    MonthlyTransactionsSummary actual =
        summaryRepository.getByAccountIdAndYearMonth(JOE_DOE_ACCOUNT_ID, now.getYear(),
            now.getMonthValue());
    MonthlyTransactionsSummary nullSummary =
        summaryRepository.getByAccountIdAndYearMonth(JOE_DOE_ACCOUNT_ID, 2021, 2);

    assertEquals(domain(), actual);
    assertNull(nullSummary);
  }

  private HMonthlyTransactionsSummary persisted() {
    return HMonthlyTransactionsSummary
        .builder()
        .year(YearMonth.now().getYear())
        .month(YearMonth.now().getMonthValue())
        .income("0/1")
        .outcome("0/1")
        .cashFlow("0/1")
        .idAccount("")
        .build();
  }

  private MonthlyTransactionsSummary domain() {
    return MonthlyTransactionsSummary.builder()
        .month(persisted().getMonth())
        .cashFlow(parseFraction(persisted().getCashFlow()))
        .income(parseFraction(persisted().getIncome()))
        .outcome(parseFraction(persisted().getOutcome()))
        .updatedAt(persisted().getUpdatedAt())
        .build();
  }

  private MonthlyTransactionsSummary updated() {
    return MonthlyTransactionsSummary
        .builder()
        .id("random_id")
        .outcome(new Fraction())
        .income(new Fraction())
        .cashFlow(new Fraction())
        .month(YearMonth.now().getMonthValue())
        .build();
  }
}
