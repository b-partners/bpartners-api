package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.TypedProspectEvaluationJobInitiated;
import app.bpartners.api.endpoint.event.model.gen.ProspectEvaluationJobInitiated;
import app.bpartners.api.endpoint.rest.model.JobStatusValue;
import app.bpartners.api.endpoint.rest.model.NewInterventionOption;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.mapper.ProspectEvaluationMapper;
import app.bpartners.api.model.mapper.ProspectMapper;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.model.prospect.job.AntiHarmRules;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJob;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJobRunner;
import app.bpartners.api.repository.ProspectEvaluationJobRepository;
import app.bpartners.api.repository.ProspectEvaluationRepository;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.expressif.ProspectEvaluation;
import app.bpartners.api.repository.expressif.ProspectEvaluationInfo;
import app.bpartners.api.repository.expressif.ProspectResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static app.bpartners.api.endpoint.rest.model.JobStatusValue.FAILED;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.FINISHED;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.IN_PROGRESS;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.NOT_STARTED;
import static app.bpartners.api.endpoint.rest.model.NewInterventionOption.NEW_PROSPECT;
import static app.bpartners.api.endpoint.rest.model.NewInterventionOption.OLD_CUSTOMER;
import static app.bpartners.api.service.utils.FilterUtils.distinctByKeys;

@Service
@AllArgsConstructor
@Slf4j
public class ProspectEvaluationService {
  private final EventProducer eventProducer;
  private final ProspectEvaluationJobRepository evalJobRepository;
  private final ProspectEvaluationRepository evaluationRepository;
  private final ProspectRepository prospectRepository;
  private final CustomerService customerService;
  private final ProspectEvaluationMapper prospectEvaluationMapper;
  private final ProspectMapper prospectMapper;

  @Transactional
  public ProspectEvaluationJob getEvaluationJob(String jobId) {
    return evalJobRepository.getById(jobId);
  }

  @Transactional
  public List<ProspectEvaluationJob> getEvaluationJobs(
      String idAccountHolder,
      List<JobStatusValue> statuses) {
    if (statuses == null) {
      statuses = List.of(NOT_STARTED, IN_PROGRESS, FINISHED, FAILED);
    }
    return evalJobRepository.findAllByIdAccountHolderAndStatusesIn(idAccountHolder, statuses);
  }

  @Transactional
  public List<ProspectEvaluationJob> saveAllEvaluationJobs(List<ProspectEvaluationJob> jobs) {
    return evalJobRepository.saveAll(jobs);
  }

  @Transactional
  public List<ProspectEvaluationJob> runEvaluationJobs(String userId,
                                                       String ahId,
                                                       List<ProspectEvaluationJobRunner> jobRunners) {
    List<ProspectEvaluationJob> evaluationJobs = jobRunners.stream()
        .map(jobRunner -> prospectEvaluationMapper.toEvaluationJob(ahId, jobRunner))
        .collect(Collectors.toList());

    List<ProspectEvaluationJob> savedJobs = evalJobRepository.saveAll(evaluationJobs);

    eventProducer.accept(jobRunners.stream()
        .map(evaluationJobRunner -> new TypedProspectEvaluationJobInitiated(
            ProspectEvaluationJobInitiated.builder()
                .jobId(evaluationJobRunner.getJobId())
                .idUser(userId)
                .jobRunner(evaluationJobRunner)
                .build()))
        .collect(Collectors.toList()));

    return savedJobs;
  }

  public List<ProspectEvaluation> saveAllEvaluations(List<ProspectEvaluation> prospectEvaluations) {
    throw new NotImplementedException("Not supported");
  }

  @Transactional
  public List<ProspectResult> evaluateProspects(String ahId,
                                                AntiHarmRules antiHarmRules,
                                                List<ProspectEvaluation> prospectEvaluations,
                                                NewInterventionOption option,
                                                Double minProspectRating,
                                                Double minCustomerRating) {
    if (option == null) {
      option = NEW_PROSPECT;
    }

    List<ProspectEvaluation> customerProspectEvaluations = option != NEW_PROSPECT
        ? prospectEvaluationMapper.toCustomerProspectEvaluation(
        ahId, antiHarmRules, prospectEvaluations, customerService)
        : new ArrayList<>();

    List<ProspectResult> prospectResults =
        evaluationRepository.evaluate(
            mergeEvaluations(prospectEvaluations, customerProspectEvaluations));

    return handleInterventionOptions(option, minProspectRating, minCustomerRating, prospectResults);
  }

  @Deprecated
  @Transactional
  public List<ProspectResult> evaluateAndSaveProspects(String ahId,
                                                       AntiHarmRules antiHarmRules,
                                                       List<ProspectEvaluation> prospectsEvaluations,
                                                       NewInterventionOption option,
                                                       Double minProspectRating,
                                                       Double minCustomerRating) {
    List<ProspectResult> prospectResults =
        evaluateProspects(
            ahId, antiHarmRules, prospectsEvaluations,
            option, minProspectRating, minCustomerRating);

    List<Prospect> newProspects =
        option == OLD_CUSTOMER ? new ArrayList<>()
            : ratedNewProspects(prospectResults, minProspectRating);

    List<Prospect> customerProspects =
        option != NEW_PROSPECT ? ratedProspectCustomers(prospectResults, minCustomerRating)
            : new ArrayList<>();

    List<Prospect> prospectsWithoutDuplication =
        mergeProspects(newProspects, customerProspects).stream()
            .filter(distinctByKeys(
                Prospect::getName,
                Prospect::getEmail,
                Prospect::getPhone,
                Prospect::getAddress))
            .collect(Collectors.toList());
    prospectRepository.createAll(prospectsWithoutDuplication);

    return handleInterventionOptions(option, minProspectRating, minCustomerRating, prospectResults);
  }

  @Transactional
  public List<ProspectEvaluation> readEvaluationsByOwnerId(String ownerId,
                                                           String spreadsheetName,
                                                           String sheetName,
                                                           Integer minRange,
                                                           Integer maxRange) {
    return readEvaluations(
        spreadsheetName,
        sheetName,
        minRange,
        maxRange).stream()
        .filter(prospect -> prospect.getProspectOwnerId().equals(ownerId))
        .collect(Collectors.toList());
  }

  public List<ProspectEvaluation> readEvaluations(String spreadsheetName,
                                                  String sheetName,
                                                  Integer minRange,
                                                  Integer maxRange) {
    return evaluationRepository.findBySpreadsheet(spreadsheetName, sheetName, minRange, maxRange);
  }

  private List<ProspectResult> handleInterventionOptions(NewInterventionOption option,
                                                         Double minProspectRating,
                                                         Double minCustomerRating,
                                                         List<ProspectResult> prospectResults) {
    List<ProspectResult> prospectWithoutDuplication = removeDuplications(prospectResults);

    List<ProspectResult> filteredRatingResults = prospectWithoutDuplication.stream()
        .filter(result -> (result.getInterventionResult() != null
            && result.getInterventionResult().getRating() >= minProspectRating)
            || (result.getCustomerInterventionResult() != null
            && result.getCustomerInterventionResult().getRating() >= minCustomerRating))
        .collect(Collectors.toList());
    switch (option) {
      case OLD_CUSTOMER:
        return filterCustomers(filteredRatingResults);
      case ALL:
        return filteredRatingResults;
      default:
        return filterNewProspects(filteredRatingResults);
    }
  }

  private List<Prospect> ratedNewProspects(List<ProspectResult> prospectResults,
                                           Double minProspectRating) {
    List<ProspectResult> ratedResults = prospectResults.stream()
        .filter(result -> result.getCustomerInterventionResult().getRating() >= minProspectRating)
        .toList();

    return prospectMapper.toNewProspect(ratedResults, minProspectRating);
  }

  private List<Prospect> ratedProspectCustomers(List<ProspectResult> prospectResults,
                                                Double minCustomerRating) {
    List<ProspectResult> ratedResults = prospectResults.stream()
        .filter(result -> result.getCustomerInterventionResult().getRating() >= minCustomerRating)
        .collect(Collectors.toList());

    return prospectMapper.toCustomerProspect(ratedResults, customerService);
  }

  private List<Prospect> mergeProspects(List<Prospect> newProspects,
                                        List<Prospect> oldCustomers) {
    List<Prospect> allProspects = new ArrayList<>(newProspects);
    allProspects.addAll(oldCustomers);
    return allProspects;
  }

  private List<ProspectResult> filterCustomers(
      List<ProspectResult> prospectResults) {
    return prospectResults.stream()
        .filter(result -> result.getCustomerInterventionResult() != null)
        .collect(Collectors.toList());
  }

  private List<ProspectResult> filterNewProspects
      (List<ProspectResult> prospectResults) {
    return prospectResults.stream()
        .filter(result -> result.getInterventionResult() != null
            && result.getCustomerInterventionResult() == null)
        .collect(Collectors.toList());
  }

  private List<ProspectEvaluation> mergeEvaluations(
      List<ProspectEvaluation> prospectEvaluations,
      List<ProspectEvaluation> oldCustomersEval) {
    List<ProspectEvaluation> allEval = new ArrayList<>(prospectEvaluations);
    allEval.addAll(oldCustomersEval);
    return allEval;
  }

  private List<ProspectResult> removeDuplications(List<ProspectResult> prospectResults) {
    List<ProspectResult> withoutDuplicat = new ArrayList<>();
    Set<String> seen = new HashSet<>();

    for (ProspectResult prospectResult : prospectResults) {
      ProspectEvaluation eval = prospectResult.getProspectEvaluation();
      ProspectEvaluationInfo info = eval.getProspectEvaluationInfo();
      ProspectResult.CustomerInterventionResult customerResult =
          prospectResult.getCustomerInterventionResult();
      Customer customerInfo = customerResult == null ? null
          : customerResult.getOldCustomer();
      String prospectName = customerInfo == null ? info.getName()
          : customerInfo.getName();
      String prospectEmail = customerInfo == null ? info.getEmail()
          : customerInfo.getEmail();
      String prospectPhone = customerInfo == null ? info.getPhoneNumber()
          : customerInfo.getPhone();
      String prospectAddress = customerInfo == null ? info.getAddress()
          : customerInfo.getFullAddress();
      String key = prospectName + ":"
          + prospectEmail + ":"
          + prospectPhone + ":"
          + prospectAddress;

      if (!seen.contains(key)) {
        seen.add(key);
        withoutDuplicat.add(prospectResult);
      }
    }
    return withoutDuplicat;
  }
}
