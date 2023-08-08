package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.NewInterventionOption;
import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Prospect;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.ProspectEvalInfo;
import app.bpartners.api.repository.expressif.ProspectResult;
import app.bpartners.api.repository.expressif.fact.NewIntervention;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.dataprocesser.ProspectDataProcesser;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import static app.bpartners.api.endpoint.rest.model.NewInterventionOption.NEW_PROSPECT;
import static app.bpartners.api.endpoint.rest.model.NewInterventionOption.OLD_CUSTOMER;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.utils.TemplateResolverUtils.parseTemplateResolver;
import static java.util.UUID.randomUUID;

@Service
@AllArgsConstructor
@Slf4j
public class ProspectService {
  public static final String PROSPECT_MAIL_TEMPLATE = "prospect_mail";
  public static final int DEFAULT_RATING_PROSPECT_TO_CONVERT = 8;
  public static final int MAX_DISTANCE_LIMIT = 1_000;
  private final ProspectRepository repository;
  private final ProspectDataProcesser dataProcesser;
  private final AccountHolderJpaRepository accountHolderJpaRepository;
  private final SesService sesService;
  private final CustomerService customerService;

  public List<Prospect> getAllByIdAccountHolder(String idAccountHolder) {
    return dataProcesser.processProspects(repository.findAllByIdAccountHolder(idAccountHolder));
  }

  public List<Prospect> saveAll(List<Prospect> toCreate) {
    return repository.saveAll(toCreate);
  }

  @Scheduled(cron = Scheduled.CRON_DISABLED, zone = "Europe/Paris")
  public void prospect() {
    accountHolderJpaRepository.findAll().forEach(accountHolder -> {
      if (repository.needsProspects(accountHolder.getId(), LocalDate.now())
          && repository.isSogefiProspector(accountHolder.getId())) {
        final String subject = "Avez-vous besoin de nouveaux clients ?";
        final String htmlbody =
            parseTemplateResolver(PROSPECT_MAIL_TEMPLATE, configureProspectContext(accountHolder));
        try {
          log.info("The email should be sent to: " + accountHolder.getEmail());
          sesService.sendEmail(accountHolder.getEmail(), null, subject, htmlbody, List.of());
        } catch (IOException | MessagingException e) {
          throw new ApiException(SERVER_EXCEPTION, e);
        }
      }
    });
  }

  //TODO: IMPORTANT ! Only NewIntervention rule is supported for now
  @Transactional
  public List<ProspectResult> evaluateProspects(List<ProspectEval> prospectsEval,
                                                NewInterventionOption option) {
    if (option == null) {
      option = NEW_PROSPECT;
    }
    boolean isNotNewProspect = option != NEW_PROSPECT;
    boolean isOldCustomer = option == OLD_CUSTOMER;

    var oldCustomersEval = isNotNewProspect ? getCustomersEval(prospectsEval)
        : new ArrayList<ProspectEval>();
    var prospectResults = repository.evaluate(mergeEvals(prospectsEval, oldCustomersEval));

    var newProspects = isOldCustomer ? new ArrayList<Prospect>()
        : retrieveProspects(prospectResults);
    var oldCustomerProspects = isNotNewProspect ? retrieveOldCustomers(prospectResults)
        : new ArrayList<Prospect>();

    repository.create(mergeProspects(newProspects, oldCustomerProspects));

    switch (option) {
      case OLD_CUSTOMER:
        return filteredCustomers(prospectResults);
      case ALL:
        return prospectResults;
      default:
        return filteredNewProspects(prospectResults);
    }
  }

  private List<Prospect> retrieveOldCustomers(List<ProspectResult> prospectResults) {
    List<ProspectResult> filteredResults = ratedCustomers(prospectResults);
    return convertToProspects(filteredResults);
  }

  private static List<ProspectResult> ratedCustomers(
      List<ProspectResult> prospectResults) {
    return filteredCustomers(prospectResults).stream()
        .filter(result -> result.getCustomerInterventionResult().getRating()
            >= DEFAULT_RATING_PROSPECT_TO_CONVERT)
        .collect(Collectors.toList());
  }

  private static List<ProspectResult> filteredCustomers(
      List<ProspectResult> prospectResults) {
    return prospectResults.stream()
        .filter(result -> result.getCustomerInterventionResult() != null)
        .collect(Collectors.toList());
  }

  private List<Prospect> retrieveProspects(List<ProspectResult> prospectResults) {
    return ratedProspects(prospectResults).stream()
        .map(this::convertNewProspect)
        .collect(Collectors.toList());
  }

  private static List<ProspectResult> ratedProspects(
      List<ProspectResult> prospectResults) {
    return filteredNewProspects(prospectResults).stream()
        .filter(result -> result.getInterventionResult().getRating()
            >= DEFAULT_RATING_PROSPECT_TO_CONVERT)
        .collect(Collectors.toList());
  }

  private static List<ProspectResult> filteredNewProspects(List<ProspectResult> prospectResults) {
    return prospectResults.stream()
        .filter(result -> result.getInterventionResult() != null
            && result.getCustomerInterventionResult() == null)
        .collect(Collectors.toList());
  }

  private static List<Prospect> mergeProspects(List<Prospect> newProspects,
                                               List<Prospect> oldCustomers) {
    List<Prospect> allProspects = new ArrayList<>(newProspects);
    allProspects.addAll(oldCustomers);
    return allProspects;
  }

  private static List<ProspectEval> mergeEvals(List<ProspectEval> prospectEvals,
                                               List<ProspectEval> oldCustomersEval) {
    List<ProspectEval> allEval = new ArrayList<>(prospectEvals);
    allEval.addAll(oldCustomersEval);
    return allEval;
  }

  private List<Prospect> convertToProspects(List<ProspectResult> prospectResults) {
    HashMap<String, List<ProspectResult>> groupByCustomer =
        dispatchResultByCustomer(prospectResults);
    List<Prospect> prospects = new ArrayList<>();
    for (Map.Entry<String, List<ProspectResult>> entry : groupByCustomer.entrySet()) {
      String idCustomer = entry.getKey();
      if (idCustomer != null) {
        Customer customer = customerService.getCustomerById(idCustomer);
        List<ProspectResult> subList = entry.getValue();
        for (ProspectResult result : subList) {
          prospects.add(convertOldCustomer(result, customer));
        }
      } else {
        log.info("Prospects results were customer null {}", entry.getValue());
      }
    }
    return prospects;
  }

  private List<ProspectEval> getCustomersEval(List<ProspectEval> prospectEvals) {
    HashMap<String, List<ProspectEval>> groupByOwner = dispatchEvalByOwner(prospectEvals);
    List<ProspectEval> oldCustomerEvals = new ArrayList<>();
    for (Map.Entry<String, List<ProspectEval>> entry : groupByOwner.entrySet()) {
      String accountHolderId = entry.getKey();
      List<ProspectEval> subList = entry.getValue();
      List<Customer> oldCustomers = customerService.findByAccountHolderId(accountHolderId);
      for (ProspectEval eval : subList) {
        NewIntervention newIntervention = (NewIntervention) eval.getDepaRule();
        for (Customer customer : oldCustomers) {
          Double distance = newIntervention.getCoordinate()
              .getDistanceFrom(customer.getLocation().getCoordinate());
          if (distance < MAX_DISTANCE_LIMIT) {
            oldCustomerEvals.add(eval.toBuilder()
                .id(String.valueOf(randomUUID())) //new ID
                .depaRule(newIntervention.toBuilder()
                    .oldCustomer(newIntervention.getOldCustomer().toBuilder()
                        .idCustomer(customer.getId())
                        .oldCustomerAddress(customer.getAddress())
                        .distNewIntAndOldCustomer(distance)
                        .build())
                    .build())
                .build());
          }
        }
      }
    }
    return oldCustomerEvals;
  }

  private HashMap<String, List<ProspectResult>> dispatchResultByCustomer(
      List<ProspectResult> prospects) {
    HashMap<String, List<ProspectResult>> prospectResultMap = new HashMap<>();
    for (ProspectResult result : prospects) {
      String idCustomer = result.getCustomerInterventionResult().getIdCustomer();
      if (idCustomer != null) {
        if (!prospectResultMap.containsKey(idCustomer)) {
          List<ProspectResult> subList = new ArrayList<>();
          subList.add(result);
          prospectResultMap.put(idCustomer, subList);
        } else {
          prospectResultMap.get(idCustomer).add(result);
        }
      }
    }
    return prospectResultMap;
  }


  private HashMap<String, List<ProspectEval>> dispatchEvalByOwner(
      List<ProspectEval> prospectEvals) {
    HashMap<String, List<ProspectEval>> prospectMap = new HashMap<>();
    for (ProspectEval prospectEval : prospectEvals) {
      if (prospectEval.isNewIntervention()) {
        String accountHolderId = prospectEval.getProspectOwnerId();
        if (!prospectMap.containsKey(accountHolderId)) {
          List<ProspectEval> subList = new ArrayList<>();
          subList.add(prospectEval);
          prospectMap.put(accountHolderId, subList);
        } else {
          prospectMap.get(accountHolderId).add(prospectEval);
        }
      }
    }
    return prospectMap;
  }

  public Prospect convertNewProspect(ProspectResult result) {
    ProspectEval eval = result.getProspectEval();
    ProspectEvalInfo info =
        result.getProspectEval().getProspectEvalInfo();
    return Prospect.builder()
        .id(String.valueOf(randomUUID())) //TODO: change when prospect eval can be override
        .idHolderOwner(eval.getProspectOwnerId())
        .name(info.getName())
        .email(info.getEmail())
        .phone(info.getPhoneNumber())
        .address(info.getAddress())
        .status(ProspectStatus.TO_CONTACT) //Default when creating
        .townCode(Integer.valueOf(info.getPostalCode()))
        .location(new Geojson()
            .latitude(info.getCoordinates().getLatitude())
            .longitude(info.getCoordinates().getLongitude()))
        .rating(Prospect.ProspectRating.builder()
            .value(result.getInterventionResult().getRating())
            .lastEvaluationDate(result.getEvaluationDate())
            .build())
        .build();
  }

  public Prospect convertOldCustomer(ProspectResult result, Customer customer) {
    ProspectEval eval = result.getProspectEval();
    result.getCustomerInterventionResult().setOldCustomer(customer);
    return Prospect.builder()
        .id(String.valueOf(randomUUID())) //TODO: change when prospect eval can be override
        .idHolderOwner(eval.getProspectOwnerId())
        .name(customer.getName())
        .email(customer.getEmail())
        .phone(customer.getPhone())
        .address(customer.getFullAddress())
        .status(ProspectStatus.TO_CONTACT) //Default when creating
        .townCode(Integer.valueOf(customer.getZipCode()))
        .location(new Geojson()
            .latitude(customer.getLocation().getCoordinate().getLatitude())
            .longitude(customer.getLocation().getCoordinate().getLongitude()))
        .rating(Prospect.ProspectRating.builder()
            .value(result.getInterventionResult().getRating())
            .lastEvaluationDate(result.getEvaluationDate())
            .build())
        .build();
  }

  private Context configureProspectContext(HAccountHolder accountHolder) {
    Context context = new Context();
    context.setVariable("accountHolderEntity", accountHolder);
    return context;
  }
}