package com.example.auth_service.exception;

import com.example.auth_service.dto.response.MessageResponse;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
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
        bindingResult.addError(new FieldError("request", "email", "must not be blank"));
        Method method = ValidationTarget.class.getDeclaredMethod("handle", Object.class);
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        var response = handler.handleValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("must not be blank", response.getBody().get("email"));
    }

    @Test
    void handlesKnownAuthExceptions() {
        assertEquals(HttpStatus.BAD_REQUEST, handler.handleBadRequest(new BadRequestException("bad request")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, handler.handleBadRequest(new JwtException("jwt issue") { }).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, handler.handleUnauthorized(new BadCredentialsException("bad creds")).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, handler.handleUnauthorized(new UsernameNotFoundException("missing")).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, handler.handleForbidden(new ForbiddenOperationException("forbidden")).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, handler.handleForbidden(new DisabledException("disabled")).getStatusCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, handler.handleTooManyRequests(new TooManyRequestsException("slow down")).getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, handler.handleEmailDelivery(new EmailDeliveryException("mail", null)).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, handler.handleOauthError(new OAuth2AuthenticationException("oauth")).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, handler.handleOauthError(new InternalAuthenticationServiceException("internal")).getStatusCode());
    }

    @Test
    void handlesUnexpectedExceptions() {
        var response = handler.handleUnexpected(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        MessageResponse body = response.getBody();
        assertEquals("Unexpected error occurred", body.message());
    }

    static class ValidationTarget {
        void handle(Object request) {
        }
    }
}
