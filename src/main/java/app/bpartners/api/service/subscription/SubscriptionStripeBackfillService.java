package app.bpartners.api.service.subscription;

import static app.bpartners.api.endpoint.rest.model.UserSubscriptionCommitmentDuration.TWELVE_MONTHS;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.UserSubscriptionCommitment;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionBillingType;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.UserSubscriptionEligible;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.UserSubscriptionCommitmentJpaRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.service.credit.CreditGrantService;
import app.bpartners.api.service.event.SubscriptionPaymentInvoiceRequestedService;
import com.stripe.model.Invoice;
import com.stripe.model.SubscriptionItem;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionStripeBackfillService {
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
  private static final int DEFAULT_TRIAL_PERIOD_DAYS = 0;

  private final UserRepository userRepository;
  private final StripeFactory stripeFactory;
  private final StripeInvoiceService stripeInvoiceService;
  private final SubscriptionProductRepository subscriptionProductRepository;
  private final UserSubscriptionEligibleJpaRepository userSubscriptionEligibleJpaRepository;
  private final UserSubscriptionProductService userSubscriptionProductService;
  private final UserSubscriptionCommitmentJpaRepository userSubscriptionCommitmentJpaRepository;
  private final SubscriptionPaymentService subscriptionPaymentService;
  private final SubscriptionPaymentInvoiceRequestedService subscriptionPaymentInvoiceService;
  private final CreditGrantService creditGrantService;

  public List<UserBackfillReport> backfill(
      List<String> userIdentifiers, Instant paidSince, boolean dryRun) {
    return userIdentifiers.stream().map(userId -> backfillUser(userId, paidSince, dryRun)).toList();
  }

  private UserBackfillReport backfillUser(
      String userIdentifier, Instant paidSince, boolean dryRun) {
    var actions = new ArrayList<String>();
    var user = userRepository.getById(userIdentifier);
    var stripeCustomerIdentifier = user.getUserSubscriptionId();
    if (stripeCustomerIdentifier == null) {
      return UserBackfillReport.skipped(
          userIdentifier, user.getEmail(), "no Stripe customer attached to this user");
    }

    var paidStripeInvoices =
        stripeInvoiceService.getPaidStripeInvoicesSince(stripeCustomerIdentifier, paidSince);
    if (paidStripeInvoices.isEmpty()) {
      return UserBackfillReport.skipped(
          userIdentifier, user.getEmail(), "no paid Stripe invoice since " + paidSince);
    }

    var resolvedPlan = resolveSubscribedPlan(user);
    var firstPaidPeriodStart = firstPaidPeriodStartOf(paidStripeInvoices);

    ensureEligible(user.getId(), dryRun, actions);
    ensureSubscriptionProduct(user.getId(), resolvedPlan, firstPaidPeriodStart, dryRun, actions);
    ensureCommitment(user.getId(), resolvedPlan, firstPaidPeriodStart, dryRun, actions);

    var invoicedPeriods = new ArrayList<InvoicedPeriodReport>();
    var isFirstPaidInvoice = true;
    LocalDate simulatedCoverageEnd = null;
    for (Invoice stripeInvoice : paidStripeInvoices) {
      var report =
          replayPaidInvoice(
              stripeInvoice,
              isFirstPaidInvoice,
              simulatedCoverageEnd,
              resolvedPlan,
              dryRun,
              actions);
      invoicedPeriods.add(report);
      if (dryRun && report.coversTo() != null) {
        simulatedCoverageEnd = report.coversTo();
      }
      isFirstPaidInvoice = false;
    }

    return new UserBackfillReport(
        userIdentifier, user.getEmail(), stripeCustomerIdentifier, actions, invoicedPeriods, null);
  }

  private InvoicedPeriodReport replayPaidInvoice(
      Invoice stripeInvoice,
      boolean isFirstPaidInvoice,
      LocalDate simulatedCoverageEnd,
      Optional<ResolvedPlan> resolvedPlan,
      boolean dryRun,
      List<String> actions) {
    var optionalPayment =
        dryRun
            ? subscriptionPaymentService.previewPaidStripeInvoice(stripeInvoice)
            : subscriptionPaymentService.recordPaidStripeInvoiceWithoutInvoicing(stripeInvoice);
    if (optionalPayment.isEmpty()) {
      return InvoicedPeriodReport.notReplayable(
          stripeInvoice.getId(), "Stripe invoice carries no subscription or charges nothing");
    }
    var subscriptionPayment = optionalPayment.get();
    if (subscriptionPayment.getInvoiceId() != null) {
      return InvoicedPeriodReport.alreadyInvoiced(
          stripeInvoice.getId(), subscriptionPayment.getInvoiceId());
    }

    var preview =
        isFirstPaidInvoice
            ? subscriptionPaymentInvoiceService.previewOwnPaidPeriod(subscriptionPayment)
            : subscriptionPaymentInvoiceService.previewAssembledPeriod(
                subscriptionPayment, simulatedCoverageEnd);
    if (dryRun) {
      return InvoicedPeriodReport.planned(stripeInvoice.getId(), preview);
    }

    var createdInvoice =
        isFirstPaidInvoice
            ? Optional.of(
                subscriptionPaymentInvoiceService.invoiceOwnPaidPeriod(subscriptionPayment))
            : subscriptionPaymentInvoiceService.invoiceAssembledPeriod(subscriptionPayment.getId());
    grantCreditsOfCurrentMonthOnly(subscriptionPayment, resolvedPlan, actions);
    return InvoicedPeriodReport.issued(
        stripeInvoice.getId(),
        createdInvoice.map(app.bpartners.api.model.Invoice::getRef).orElse(null),
        preview);
  }

  private void grantCreditsOfCurrentMonthOnly(
      SubscriptionPayment subscriptionPayment,
      Optional<ResolvedPlan> resolvedPlan,
      List<String> actions) {
    if (resolvedPlan.isEmpty()) {
      return;
    }
    var periodStart = subscriptionPayment.getPeriodStartDatetime();
    if (periodStart == null
        || !YearMonth.from(periodStart.atZone(PARIS)).equals(YearMonth.now(PARIS))) {
      actions.add(
          "included credits not granted for SubscriptionPayment("
              + subscriptionPayment.getId()
              + "): billed month is not the current one");
      return;
    }
    creditGrantService.grantIncludedCreditsOfBilledMonth(
        subscriptionPayment.getUserId(), resolvedPlan.get().product(), periodStart);
    actions.add(
        "granted included credits of the current month for SubscriptionPayment("
            + subscriptionPayment.getId()
            + ")");
  }

  private void ensureEligible(String userIdentifier, boolean dryRun, List<String> actions) {
    if (userSubscriptionEligibleJpaRepository.findByUserId(userIdentifier).isPresent()) {
      return;
    }
    actions.add("create user_subscription_eligible");
    if (dryRun) {
      return;
    }
    userSubscriptionEligibleJpaRepository.save(
        UserSubscriptionEligible.builder()
            .id(randomUUID().toString())
            .userId(userIdentifier)
            .trialPeriodDays(DEFAULT_TRIAL_PERIOD_DAYS)
            .eligibleFrom(LocalDate.now(PARIS))
            .creationDatetime(now())
            .build());
  }

  private void ensureSubscriptionProduct(
      String userIdentifier,
      Optional<ResolvedPlan> resolvedPlan,
      LocalDate subscriptionStart,
      boolean dryRun,
      List<String> actions) {
    if (resolvedPlan.isEmpty()) {
      actions.add(
          "no subscription_product matches the Stripe plan, user_subscription_product left as is");
      return;
    }
    if (userSubscriptionProductService
        .findActiveUserSubscriptionProduct(userIdentifier)
        .isPresent()) {
      return;
    }
    var plan = resolvedPlan.get();
    actions.add(
        "create user_subscription_product on plan "
            + plan.product().getName()
            + " billed "
            + plan.billingInterval());
    if (dryRun) {
      return;
    }
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        userIdentifier,
        plan.product().getId(),
        plan.billingInterval(),
        subscriptionStart.atStartOfDay(PARIS).toInstant());
  }

  private void ensureCommitment(
      String userIdentifier,
      Optional<ResolvedPlan> resolvedPlan,
      LocalDate commitmentStart,
      boolean dryRun,
      List<String> actions) {
    if (resolvedPlan.isEmpty()
        || !SubscriptionBillingType.COMMITMENT.equals(
            resolvedPlan.get().product().getBillingType())) {
      return;
    }
    if (!userSubscriptionCommitmentJpaRepository.findAllByUserId(userIdentifier).isEmpty()) {
      return;
    }
    var commitmentEnd = commitmentStart.plusYears(1);
    actions.add(
        "create user_subscription_commitment from " + commitmentStart + " to " + commitmentEnd);
    if (dryRun) {
      return;
    }
    userSubscriptionCommitmentJpaRepository.save(
        UserSubscriptionCommitment.builder()
            .id(randomUUID().toString())
            .userId(userIdentifier)
            .subscriptionPlanIdentifier(resolvedPlan.get().product().getId())
            .duration(TWELVE_MONTHS)
            .approvalDatetime(commitmentStart.atStartOfDay(PARIS).toInstant())
            .commitmentStartDatetime(commitmentStart.atStartOfDay(PARIS).toInstant())
            .commitmentEndDatetime(commitmentEnd.atStartOfDay(PARIS).toInstant())
            .creationDatetime(now())
            .build());
  }

  private LocalDate firstPaidPeriodStartOf(List<Invoice> paidStripeInvoices) {
    return paidStripeInvoices.stream()
        .map(this::periodStartOf)
        .min(Comparator.naturalOrder())
        .orElseThrow();
  }

  private LocalDate periodStartOf(Invoice stripeInvoice) {
    var lines = stripeInvoice.getLines() == null ? null : stripeInvoice.getLines().getData();
    if (lines != null) {
      var earliestLineStart =
          lines.stream()
              .filter(line -> line.getPeriod() != null && line.getPeriod().getStart() != null)
              .map(line -> Instant.ofEpochSecond(line.getPeriod().getStart()))
              .min(Comparator.naturalOrder());
      if (earliestLineStart.isPresent()) {
        return earliestLineStart.get().atZone(PARIS).toLocalDate();
      }
    }
    var periodStart =
        stripeInvoice.getPeriodStart() == null
            ? stripeInvoice.getCreated()
            : stripeInvoice.getPeriodStart();
    return Instant.ofEpochSecond(periodStart).atZone(PARIS).toLocalDate();
  }

  private Optional<ResolvedPlan> resolveSubscribedPlan(app.bpartners.api.model.User user) {
    try {
      return stripeFactory.retrieveUserSubscriptions(user).stream()
          .flatMap(subscription -> subscription.getItems().getData().stream())
          .map(this::resolvePlanFromItem)
          .flatMap(Optional::stream)
          .findFirst();
    } catch (Exception e) {
      log.warn("Unable to resolve the Stripe plan of User(id={})", user.getId(), e);
      return Optional.empty();
    }
  }

  private Optional<ResolvedPlan> resolvePlanFromItem(SubscriptionItem subscriptionItem) {
    var stripeProductIdentifier =
        subscriptionItem.getPlan() == null ? null : subscriptionItem.getPlan().getProduct();
    if (stripeProductIdentifier == null) {
      return Optional.empty();
    }
    return subscriptionProductRepository
        .findByE2Id(stripeProductIdentifier)
        .map(plan -> new ResolvedPlan(plan, billingIntervalOf(subscriptionItem)));
  }

  private BillingInterval billingIntervalOf(SubscriptionItem subscriptionItem) {
    var interval =
        subscriptionItem.getPlan() == null ? null : subscriptionItem.getPlan().getInterval();
    return "year".equals(interval) ? BillingInterval.YEARLY : BillingInterval.MONTHLY;
  }

  private record ResolvedPlan(SubscriptionProduct product, BillingInterval billingInterval) {}

  public record UserBackfillReport(
      String userId,
      String email,
      String stripeCustomerId,
      List<String> plannedOrAppliedActions,
      List<InvoicedPeriodReport> invoices,
      String skippedBecause) {
    private static UserBackfillReport skipped(String userId, String email, String reason) {
      return new UserBackfillReport(userId, email, null, List.of(), List.of(), reason);
    }
  }

  public record InvoicedPeriodReport(
      String stripeInvoiceId,
      String outcome,
      String invoiceRef,
      LocalDate coversFrom,
      LocalDate coversTo,
      int lineCount) {
    private static InvoicedPeriodReport notReplayable(String stripeInvoiceId, String reason) {
      return new InvoicedPeriodReport(stripeInvoiceId, reason, null, null, null, 0);
    }

    private static InvoicedPeriodReport alreadyInvoiced(String stripeInvoiceId, String invoiceId) {
      return new InvoicedPeriodReport(
          stripeInvoiceId, "ALREADY_INVOICED", invoiceId, null, null, 0);
    }

    private static InvoicedPeriodReport planned(
        String stripeInvoiceId, SubscriptionPaymentInvoiceRequestedService.BillingPreview preview) {
      return new InvoicedPeriodReport(
          stripeInvoiceId,
          preview.alreadyCovered() ? "WOULD_ATTACH_TO_COVERING_INVOICE" : "WOULD_ISSUE",
          null,
          preview.coversFrom(),
          preview.coversTo(),
          preview.lineCount());
    }

    private static InvoicedPeriodReport issued(
        String stripeInvoiceId,
        String invoiceRef,
        SubscriptionPaymentInvoiceRequestedService.BillingPreview preview) {
      return new InvoicedPeriodReport(
          stripeInvoiceId,
          preview.alreadyCovered() ? "ATTACHED_TO_COVERING_INVOICE" : "ISSUED",
          invoiceRef,
          preview.coversFrom(),
          preview.coversTo(),
          preview.lineCount());
    }
  }
}
