package app.bpartners.api.unit.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.AnnualRevenueTarget;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.repository.AnnualRevenueTargetRepository;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AnnualRevenueTargetRepositoryTest {

  @Mock private AnnualRevenueTargetRepository repository;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testSaveAll() {
    List<AnnualRevenueTarget> targetsToSave =
        Arrays.asList(
            AnnualRevenueTarget.builder()
                .id("1")
                .year(2024)
                .idAccountHolder("account1")
                .amountTarget(new Fraction(BigInteger.valueOf(5000), BigInteger.ONE))
                .updatedAt(Instant.now())
                .amountAttemptedPercent(
                    new Fraction(BigInteger.valueOf(50), BigInteger.valueOf(100)))
                .amountAttempted(new Fraction(BigInteger.valueOf(2500), BigInteger.ONE))
                .build(),
            AnnualRevenueTarget.builder()
                .id("2")
                .year(2025)
                .idAccountHolder("account2")
                .amountTarget(new Fraction(BigInteger.valueOf(10000), BigInteger.ONE))
                .updatedAt(Instant.now())
                .amountAttemptedPercent(
                    new Fraction(BigInteger.valueOf(75), BigInteger.valueOf(100)))
                .amountAttempted(new Fraction(BigInteger.valueOf(7500), BigInteger.ONE))
                .build());

    when(repository.saveAll(targetsToSave)).thenReturn(targetsToSave);
    List<AnnualRevenueTarget> savedTargets = repository.saveAll(targetsToSave);

    assertNotNull(savedTargets);
    assertEquals(2, savedTargets.size());
    verify(repository, times(1)).saveAll(targetsToSave);
  }

  @Test
  void testGetAnnualRevenueTargets() {
    String accountHolderId = "account1";
    List<AnnualRevenueTarget> expectedTargets =
        Arrays.asList(
            AnnualRevenueTarget.builder()
                .id("1")
                .year(2024)
                .idAccountHolder(accountHolderId)
                .amountTarget(new Fraction(BigInteger.valueOf(5000), BigInteger.ONE))
                .updatedAt(Instant.now())
                .amountAttemptedPercent(
                    new Fraction(BigInteger.valueOf(50), BigInteger.valueOf(100)))
                .amountAttempted(new Fraction(BigInteger.valueOf(2500), BigInteger.ONE))
                .build());

    when(repository.getAnnualRevenueTargets(accountHolderId)).thenReturn(expectedTargets);
    List<AnnualRevenueTarget> targets = repository.getAnnualRevenueTargets(accountHolderId);

    assertNotNull(targets);
    assertEquals(1, targets.size());
    assertEquals(accountHolderId, targets.get(0).getIdAccountHolder());
    verify(repository, times(1)).getAnnualRevenueTargets(accountHolderId);
  }

  @Test
  void testGetByYear() {
    String accountHolderId = "account1";
    int year = 2024;
    Optional<AnnualRevenueTarget> expectedTarget =
        Optional.of(
            AnnualRevenueTarget.builder()
                .id("1")
                .year(year)
                .idAccountHolder(accountHolderId)
                .amountTarget(new Fraction(BigInteger.valueOf(5000), BigInteger.ONE))
                .updatedAt(Instant.now())
                .amountAttemptedPercent(
                    new Fraction(BigInteger.valueOf(50), BigInteger.valueOf(100)))
                .amountAttempted(new Fraction(BigInteger.valueOf(2500), BigInteger.ONE))
                .build());

    when(repository.getByYear(accountHolderId, year)).thenReturn(expectedTarget);
    Optional<AnnualRevenueTarget> target = repository.getByYear(accountHolderId, year);

    assertTrue(target.isPresent());
    assertEquals(year, target.get().getYear());
    assertEquals(accountHolderId, target.get().getIdAccountHolder());
    verify(repository, times(1)).getByYear(accountHolderId, year);
  }
}
