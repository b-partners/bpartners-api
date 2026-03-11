package app.bpartners.api.model;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class UserTest {
  User subject;

  @Test
  void get_default_website() {
    var nonValidAccountHolder = AccountHolder.builder().build();
    var anotherNonValidAccountHolder = AccountHolder.builder().website("").build();
    var firstAccountHolder = AccountHolder.builder().website(randomUUID() + ".com").build();
    var secondAccountHolder = AccountHolder.builder().website(randomUUID() + ".com").build();

    subject =
        User.builder()
            .accountHolders(
                List.of(
                    nonValidAccountHolder,
                    anotherNonValidAccountHolder,
                    firstAccountHolder,
                    secondAccountHolder))
            .build();

    var actual = subject.getDefaultWebsite();

    assertEquals(firstAccountHolder.getWebsite(), actual);
  }

  @Test
  void get_default_website_null_when_no_account_holder_have_website() {
    var nonValidAccountHolder = AccountHolder.builder().build();

    subject = User.builder().accountHolders(List.of(nonValidAccountHolder)).build();

    var actual = subject.getDefaultWebsite();

    assertEquals(null, actual);
  }

  @Test
  void get_default_website_null_when_empty_account_holder() {
    subject = User.builder().accountHolders(null).build();

    var actual = subject.getDefaultWebsite();

    assertEquals(null, actual);
  }
}
