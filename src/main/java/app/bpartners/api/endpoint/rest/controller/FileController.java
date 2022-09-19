package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.model.FileInfo;
import app.bpartners.api.model.mapper.FileMapper;
import app.bpartners.api.service.FileService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;

@RestController
@AllArgsConstructor
public class FileController {
  private FileService service;
  private FileMapper mapper;

  @GetMapping(value = "/files/{id}")
  public FileInfo getFileInfoById(@PathVariable(name = "id") String fileId) {
    return mapper.toRest(service.getFileById(fileId));
  }

  @GetMapping(value = "/files/{id}/raw",
      produces = {IMAGE_JPEG_VALUE})
  public byte[] downloadFile(
      @PathVariable(name = "id") String fileId) {
    return service.downloadFile(fileId);
  }

  @PostMapping(value = "/files/{id}/raw",
      produces = {IMAGE_JPEG_VALUE},
      consumes = {IMAGE_JPEG_VALUE, APPLICATION_JSON_VALUE})
  public byte[] uploadFile(
      @PathVariable(name = "id") String fileId,
      @RequestBody byte[] toUpload) {
    return service.uploadFile(fileId, toUpload);
  }
}