package app.bpartners.api.unit.validator;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.validator.AccountHolderValidator;
import app.bpartners.api.service.accountholder.AccountHolderInvoiceValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AccountHolderInvoiceValidatorTest {
    AccountHolderInvoiceValidator subject = new AccountHolderInvoiceValidator();

    @Test
    void subject_address_null(){
        var accountHolder = AccountHolder.builder().build();

        var actual = assertThrows(BadRequestException.class, () -> {subject.accept(accountHolder);});
        var expect = "Account holder address is mandatory to confirm invoiceAccount holder country is mandatory to confirm invoiceAccount holder city is mandatory to confirm invoiceAccount holder postal code is mandatory to confirm invoice";
        assertEquals(expect, actual.getMessage());
    }
}
