package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.model.FileInfo;
import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.mapper.FileMapper;
import app.bpartners.api.service.FileService;
import app.bpartners.api.service.utils.FileInfoUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Slf4j
public class FileController {
  private FileService service;
  private FileMapper mapper;

  @GetMapping(value = "/accounts/{accountId}/files/{id}")
  public FileInfo getFileInfoById(
      @PathVariable(name = "accountId") String accountId,
      @PathVariable(name = "id") String fileId) {
    return mapper.toRest(service.findById(fileId));
  }

  @GetMapping(value = "/accounts/{accountId}/files/{id}/raw")
  public ResponseEntity<byte[]> downloadFile(
      @PathVariable(name = "accountId") String accountId,
      @PathVariable(name = "id") String fileId,
      @RequestParam(name = "fileType") FileType fileType) {
    String idUser =
        AuthProvider.getAuthenticatedUserId(); // TODO: should be changed when endpoint changed
    byte[] downloaded = service.downloadFile(fileType, idUser, fileId);
    return ResponseEntity.ok()
        .contentType(FileInfoUtils.parseMediaTypeFromBytes(downloaded))
        .body(downloaded);
  }

  @PostMapping(value = "/accounts/{accountId}/files/{id}/raw")
  public ResponseEntity<byte[]> uploadFile(
      @PathVariable(name = "accountId") String accountId,
      @PathVariable(name = "id") String fileId,
      @RequestParam(name = "fileType") FileType fileType,
      @RequestBody byte[] toUpload) {
    String idUser =
        AuthProvider.getAuthenticatedUserId(); // TODO: should be changed when endpoint changed
    service.upload(fileId, fileType, idUser, toUpload);
    return ResponseEntity.ok()
        .contentType(FileInfoUtils.parseMediaTypeFromBytes(toUpload))
        .body(toUpload);
  }
}
