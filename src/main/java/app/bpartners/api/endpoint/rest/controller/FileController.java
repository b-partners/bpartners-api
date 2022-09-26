package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.model.FileInfo;
import app.bpartners.api.model.mapper.FileMapper;
import app.bpartners.api.service.FileService;
import app.bpartners.api.service.utils.FileInfoUtils;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@RestController
@AllArgsConstructor
public class FileController {
  private FileService service;
  private FileMapper mapper;

  @GetMapping(value = "/files/{id}")
  public FileInfo getFileInfoById(@PathVariable(name = "id") String fileId) {
    return mapper.toRest(service.getFileById(fileId));
  }

  @GetMapping(value = "/files/{id}/raw")
  public ResponseEntity<byte[]> downloadFile(
      @PathVariable(name = "id") String fileId) {
    ResponseBytes<GetObjectResponse> downloaded = service.downloadFile(fileId);
    return ResponseEntity.ok()
        .contentType(MediaType.valueOf(downloaded.response().contentType()))
        .body(downloaded.asByteArray());
  }

  @PostMapping(value = "/files/{id}/raw")
  public ResponseEntity<byte[]> uploadFile(
      @PathVariable(name = "id") String fileId,
      @RequestBody byte[] toUpload) {
    byte[] uploaded = service.uploadFile(fileId, toUpload);
    return ResponseEntity.ok()
        .contentType(FileInfoUtils.parseMediaType(fileId))
        .body(uploaded);
  }
}