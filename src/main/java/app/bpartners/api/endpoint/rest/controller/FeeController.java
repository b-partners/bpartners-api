package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.model.BoundedPageSize;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import app.bpartners.api.endpoint.rest.mapper.FeeMapper;
import app.bpartners.api.endpoint.rest.model.CreateFee;
import app.bpartners.api.endpoint.rest.model.Fee;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.service.FeeService;

import static java.util.stream.Collectors.toUnmodifiableList;

@RestController
@AllArgsConstructor
public class FeeController {

  private final FeeService feeService;
  private final FeeMapper feeMapper;

  @GetMapping("/students/{studentId}/fees/{feeId}")
  public Fee getFeeByStudentId(
      @PathVariable String studentId,
      @PathVariable String feeId) {
    return feeMapper.toRestFee(feeService.getByStudentIdAndFeeId(studentId, feeId));
  }

  @PostMapping("/students/{studentId}/fees")
  public List<Fee> createFees(
      @PathVariable String studentId, @RequestBody List<CreateFee> toCreate) {
    return feeService.saveAll(
            feeMapper.toDomainFee(studentId, toCreate)).stream()
        .map(feeMapper::toRestFee)
        .collect(toUnmodifiableList());
  }

  @GetMapping("/students/{studentId}/fees")
  public List<Fee> getFeesByStudentId(
      @PathVariable String studentId,
      @RequestParam PageFromOne page,
      @RequestParam("page_size") BoundedPageSize pageSize) {
    return feeService.getFeesByStudentId(studentId, page, pageSize).stream()
        .map(feeMapper::toRestFee)
        .collect(toUnmodifiableList());
  }
}
