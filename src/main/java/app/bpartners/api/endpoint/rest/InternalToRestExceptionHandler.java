package app.bpartners.api.endpoint.rest;

import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.exception.TooManyRequestsException;
import javax.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


@RestControllerAdvice
@Slf4j
public class InternalToRestExceptionHandler {

  @ExceptionHandler(value = {BadRequestException.class})
  ResponseEntity<app.bpartners.api.endpoint.rest.model.Exception> handleBadRequest(
      BadRequestException e) {
    log.info("Bad request", e);
    return new ResponseEntity<>(toRest(e, HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(value = {MissingServletRequestParameterException.class})
  ResponseEntity<app.bpartners.api.endpoint.rest.model.Exception> handleBadRequest(
      MissingServletRequestParameterException e) {
    log.info("Missing parameter", e);
    return handleBadRequest(new BadRequestException(e.getMessage()));
  }

  @ExceptionHandler(value = {HttpRequestMethodNotSupportedException.class})
  ResponseEntity<app.bpartners.api.endpoint.rest.model.Exception> handleBadRequest(
      HttpRequestMethodNotSupportedException e) {
    log.info("Unsupported method for this endpoint", e);
    return handleBadRequest(new BadRequestException(e.getMessage()));
  }

  @ExceptionHandler(value = {HttpMessageNotReadableException.class})
  ResponseEntity<app.bpartners.api.endpoint.rest.model.Exception> handleBadRequest(
      HttpMessageNotReadableException e) {
    log.info("Missing required body", e);
    return handleBadRequest(new BadRequestException(e.getMessage()));
  }

  @ExceptionHandler(value = {MethodArgumentTypeMismatchException.class})
  ResponseEntity<app.bpartners.api.endpoint.rest.model.Exception> handleConversionFailed(
      MethodArgumentTypeMismatchException e) {
    log.info("Conversion failed", e);
    String message = e.getCause().getCause().getMessage();
    return handleBadRequest(new BadRequestException(message));
  }

  @ExceptionHandler(value = {TooManyRequestsException.class})
  ResponseEntity<app.bpartners.api.endpoint.rest.model.Exception> handleTooManyRequests(
      TooManyRequestsException e) {
    log.info("Too many requests", e);
    return new ResponseEntity<>(
        toRest(e, HttpStatus.TOO_MANY_REQUESTS),
        HttpStatus.TOO_MANY_REQUESTS);
  }

  @ExceptionHandler(
      value = {
          LockAcquisitionException.class,
          CannotAcquireLockException.class,
          OptimisticLockException.class
      })
  ResponseEntity<app.bpartners.api.endpoint.rest.model.Exception> handleLockAcquisitionException(
      Exception e) {
    log.warn("Database lock could not be acquired: too many requests assumed", e);
    return handleTooManyRequests(new TooManyRequestsException(e));
  }

  @ExceptionHandler(value = {
      AccessDeniedException.class,
      BadCredentialsException.class,
      ForbiddenException.class})
  ResponseEntity<app.bpartners.api.endpoint.rest.model.Exception> handleForbidden(Exception e) {
    /* rest.model.Exception.Type.FORBIDDEN designates both authentication and authorization errors.
     * Hence do _not_ HttpsStatus.UNAUTHORIZED because, counter-intuitively,
     * it's just for authentication.
     * https://stackoverflow.com/questions/3297048/403-forbidden-vs-401-unauthorized-http-responses */
    log.info("Forbidden", e);
    var restException = new app.bpartners.api.endpoint.rest.model.Exception();
    restException.setType(HttpStatus.FORBIDDEN.toString());
    restException.setMessage(e.getMessage());
    return new ResponseEntity<>(restException, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(value = {NotFoundException.class})
  ResponseEntity<app.bpartners.api.endpoint.rest.model.Exception> handleNotFound(
      NotFoundException e) {
    log.info("Not found", e);
    return new ResponseEntity<>(toRest(e, HttpStatus.NOT_FOUND), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(value = {NotImplementedException.class})
  ResponseEntity<app.bpartners.api.endpoint.rest.model.Exception> handleNotImplemented(
      NotImplementedException e) {
    log.error("Not implemented", e);
    return new ResponseEntity<>(toRest(e, HttpStatus.NOT_IMPLEMENTED), HttpStatus.NOT_IMPLEMENTED);
  }

  @ExceptionHandler(value = {DataIntegrityViolationException.class})
  ResponseEntity<app.bpartners.api.endpoint.rest.model.Exception> handleDataIntegrityViolation(
      DataIntegrityViolationException e) {
    log.info("Bad request", e);
    return new ResponseEntity<>(toRest(e, HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(value = {Exception.class})
  ResponseEntity<app.bpartners.api.endpoint.rest.model.Exception> handleDefault(Exception e) {
    log.error("Internal error", e);
    return new ResponseEntity<>(
        toRest(e, HttpStatus.INTERNAL_SERVER_ERROR),
        HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private app.bpartners.api.endpoint.rest.model.Exception toRest(Exception e, HttpStatus status) {
    var restException = new app.bpartners.api.endpoint.rest.model.Exception();
    restException.setType(status.toString());
    restException.setMessage(e.getMessage());
    return restException;
  }
}
