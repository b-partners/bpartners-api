package app.bpartners.api.unit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.mapper.AccountHolderMapper;
import app.bpartners.api.model.validator.AccountHolderValidator;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import org.junit.jupiter.api.Test;

class AccountHolderMapperTest {
  private static final String OTHER_CONCERNED_EMAIL = "concerned@birdia.fr";

  AccountHolderMapper subject = new AccountHolderMapper(new AccountHolderValidator());

  @Test
  void to_domain_maps_other_concerned_email() {
    var entity = entityBuilder().otherConcernedEmail(OTHER_CONCERNED_EMAIL).build();

    assertEquals(OTHER_CONCERNED_EMAIL, subject.toDomain(entity).getOtherConcernedEmail());
  }

  @Test
  void to_domain_keeps_null_other_concerned_email() {
    var entity = entityBuilder().otherConcernedEmail(null).build();

    assertNull(subject.toDomain(entity).getOtherConcernedEmail());
  }

  @Test
  void to_entity_maps_other_concerned_email() {
    var domain = domainBuilder().otherConcernedEmail(OTHER_CONCERNED_EMAIL).build();

    assertEquals(OTHER_CONCERNED_EMAIL, subject.toEntity(domain).getOtherConcernedEmail());
  }

  @Test
  void to_entity_keeps_null_other_concerned_email() {
    var domain = domainBuilder().otherConcernedEmail(null).build();

    assertNull(subject.toEntity(domain).getOtherConcernedEmail());
  }

  @Test
  void other_concerned_email_survives_a_round_trip() {
    var entity = entityBuilder().otherConcernedEmail(OTHER_CONCERNED_EMAIL).build();

    var actual = subject.toEntity(subject.toDomain(entity));

    assertEquals(OTHER_CONCERNED_EMAIL, actual.getOtherConcernedEmail());
  }

  private static HAccountHolder.HAccountHolderBuilder entityBuilder() {
    return HAccountHolder.builder()
        .id("holder_id")
        .email("holder@birdia.fr")
        .socialCapital("0/1")
        .initialCashflow("0/1");
  }

  private static AccountHolder.AccountHolderBuilder domainBuilder() {
    return AccountHolder.builder()
        .id("holder_id")
        .email("holder@birdia.fr")
        .socialCapital(0)
        .initialCashflow(new Fraction());
  }
}
