package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.model.FileInfo;
import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.model.mapper.FileMapper;
import app.bpartners.api.service.FileService;
import app.bpartners.api.service.utils.FileInfoUtils;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class FileController {
  private FileService service;
  private FileMapper mapper;

  @GetMapping(value = "/accounts/{accountId}/files/{id}")
  public FileInfo getFileInfoById(
      @PathVariable(name = "accountId") String accountId,
      @PathVariable(name = "id") String fileId) {
    return mapper.toRest(service.getFileByAccountIdAndId(accountId, fileId));
  }

  @GetMapping(value = "/accounts/{accountId}/files/{id}/raw")
  public ResponseEntity<byte[]> downloadFile(
      @PathVariable(name = "accountId") String accountId,
      @PathVariable(name = "id") String fileId,
      @RequestParam(name = "fileType") FileType fileType) {
    byte[] downloaded = service.downloadFile(fileType, accountId, fileId);
    return ResponseEntity.ok()
        .contentType(FileInfoUtils.parseMediaTypeFromBytes(fileId, downloaded))
        .body(downloaded);
  }

  @PostMapping(value = "/accounts/{accountId}/files/{id}/raw")
  public ResponseEntity<byte[]> uploadFile(
      @PathVariable(name = "accountId") String accountId,
      @PathVariable(name = "id") String fileId,
      @RequestParam(name = "fileType") FileType fileType,
      @RequestBody byte[] toUpload) {
    service.uploadEvent(fileType, accountId, fileId, toUpload);
    return ResponseEntity.ok()
        .contentType(FileInfoUtils.parseMediaTypeFromBytes(fileId, toUpload))
        .body(toUpload);
  }
}