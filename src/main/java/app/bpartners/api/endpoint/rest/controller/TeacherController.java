package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.User;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import app.bpartners.api.endpoint.rest.mapper.UserMapper;
import app.bpartners.api.endpoint.rest.model.Teacher;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.service.UserService;

import static java.util.stream.Collectors.toUnmodifiableList;

@RestController
@AllArgsConstructor
public class TeacherController {

  private final UserService userService;
  private final UserMapper userMapper;

  @GetMapping(value = "/teachers/{id}")
  public Teacher getTeacherById(@PathVariable String id) {
    return userMapper.toRestTeacher(userService.getById(id));
  }

  @GetMapping(value = "/teachers")
  public List<Teacher> getTeachers(
      @RequestParam PageFromOne page, @RequestParam("page_size") BoundedPageSize pageSize,
      @RequestParam(value = "ref", required = false, defaultValue = "") String ref,
      @RequestParam(value = "first_name", required = false, defaultValue = "") String firstName,
      @RequestParam(value = "last_name", required = false, defaultValue = "") String lastName) {
    return userService.getByCriteria(User.Role.TEACHER, firstName, lastName, ref, page, pageSize
        ).stream()
        .map(userMapper::toRestTeacher)
        .collect(toUnmodifiableList());
  }

  @PutMapping(value = "/teachers")
  public List<Teacher> createOrUpdateTeachers(@RequestBody List<Teacher> toWrite) {
    return userService
        .saveAll(toWrite.stream()
            .map(userMapper::toDomain)
            .collect(toUnmodifiableList()))
        .stream()
        .map(userMapper::toRestTeacher)
        .collect(toUnmodifiableList());
  }
}
