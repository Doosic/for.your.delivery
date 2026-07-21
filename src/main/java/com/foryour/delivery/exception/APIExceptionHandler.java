package com.foryour.delivery.exception;

import com.foryour.delivery.common.APIErrorResponse;
import com.foryour.delivery.domain.enums.ErrorCode;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

import static com.foryour.delivery.domain.enums.ErrorCode.*;


@RestControllerAdvice(annotations = RestController.class)
public class APIExceptionHandler extends ResponseEntityExceptionHandler {


  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleGlobalExceptionInternal(Exception e, WebRequest request){
    return handleExceptionInternal(e, INTERNAL_ERROR, request);
  }

  @ExceptionHandler(AccountException.class)
  public ResponseEntity<Object> handleGlobalExceptionAccount(Exception e, WebRequest request){
    if(e.getMessage().equals(DUPLICATED_DATA.getMessage())){
      return handleExceptionInternal(e, DUPLICATED_DATA, request);

    } else if(e.getMessage().equals(LOCKED_ACCOUNT.getMessage())){
      return handleExceptionInternal(e, LOCKED_ACCOUNT, request);

    } else if(e.getMessage().equals(DATA_NOT_EXIST.getMessage())){
      return handleExceptionInternal(e, DATA_NOT_EXIST, request);

    } else if(e.getMessage().equals(UNAUTHORIZED_FAIL.getMessage())){
      return handleExceptionInternal(e, UNAUTHORIZED_FAIL, request);
    }

    return handleExceptionInternal(e, INTERNAL_ERROR, request);
  }

  @ExceptionHandler(APIException.class)
  public ResponseEntity<Object> handleGlobalExceptionAPI(Exception e, WebRequest request){
    if(e.getMessage().equals(DUPLICATED_DATA.getMessage())){
      return handleExceptionInternal(e, DUPLICATED_DATA, request);

    } else if(e.getMessage().equals(LOCKED_ACCOUNT.getMessage())){
      return handleExceptionInternal(e, LOCKED_ACCOUNT, request);

    } else if(e.getMessage().equals(DATA_NOT_EXIST.getMessage())){
      return handleExceptionInternal(e, DATA_NOT_EXIST, request);

    } else if(e.getMessage().equals(UNAUTHORIZED_FAIL.getMessage())){
      return handleExceptionInternal(e, UNAUTHORIZED_FAIL, request);
    }

    return handleExceptionInternal(e, INTERNAL_ERROR, request);
  }


  @ExceptionHandler(ChangeSetPersister.NotFoundException.class)
  public ResponseEntity<Object> handleGlobalExceptionNotFound(ChangeSetPersister.NotFoundException e, WebRequest request){
    return handleExceptionInternal(e, NOT_FOUND, request);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return super.handleExceptionInternal(
        ex,
        APIErrorResponse.of(
            false,
            VALIDATION_ERROR.getCode(),
            VALIDATION_ERROR.getMessage(ex.getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(",")))
        ),
        HttpHeaders.EMPTY,
        VALIDATION_ERROR.getHttpStatus(),
        request);
  }

  private ResponseEntity<Object> handleExceptionInternal(Exception e, ErrorCode errorCode, WebRequest request){
    return super.handleExceptionInternal(
        e,
        APIErrorResponse.of(false, errorCode.getCode(), errorCode.getMessage(e)),
        HttpHeaders.EMPTY,
        errorCode.getHttpStatus(),
        request
    );
  }
}
