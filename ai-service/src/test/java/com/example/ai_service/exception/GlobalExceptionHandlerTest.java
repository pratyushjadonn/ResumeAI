package com.example.ai_service.exception;

import com.example.ai_service.dto.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlesValidationErrors() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "targetRole", "must not be blank"));
        Method method = ValidationTarget.class.getDeclaredMethod("handle", Object.class);
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        var response = handler.handleValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("must not be blank", response.getBody().get("targetRole"));
    }

    @Test
    void handlesBadRequest() {
        var response = handler.handleBadRequest(new BadRequestException("bad request"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertEquals(400, body.status());
        assertEquals("Bad Request", body.error());
        assertEquals("bad request", body.message());
    }

    @Test
    void handlesQuotaExceeded() {
        var response = handler.handleQuotaExceeded(new AiQuotaException("quota exceeded"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertEquals(429, body.status());
        assertEquals("Too Many Requests", body.error());
        assertEquals("quota exceeded", body.message());
    }

    static class ValidationTarget {
        void handle(Object request) {
        }
    }
}
