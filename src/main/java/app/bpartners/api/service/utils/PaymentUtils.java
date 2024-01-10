package app.bpartners.api.service.utils;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apfloat.Aprational;

public class PaymentUtils {
  private PaymentUtils() {}

  public static Fraction computeTotalPriceFromPaymentReq(List<PaymentRequest> payments) {
    AtomicReference<Fraction> fraction = new AtomicReference<>(new Fraction());
    payments.forEach(
        payment -> fraction.set(fraction.get().operate(payment.getAmount(), Aprational::add)));
    return fraction.get();
  }

  public static Fraction computeTotalPriceFromPaymentReqEntity(List<HPaymentRequest> payments) {
    AtomicReference<Fraction> fraction = new AtomicReference<>(new Fraction());
    payments.forEach(
        payment ->
            fraction.set(
                fraction.get().operate(parseFraction(payment.getAmount()), Aprational::add)));
    return fraction.get();
  }
}
