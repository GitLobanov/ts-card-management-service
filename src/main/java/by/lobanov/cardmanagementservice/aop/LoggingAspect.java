package by.lobanov.cardmanagementservice.aop;

import by.lobanov.cardmanagementservice.model.dto.response.PagedResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingAspect {

    private final ObjectMapper objectMapper;

    /**
     * Pointcut, который выбирает все public методы во всех классах
     * в пакете controller и его подпакетах.
     */
    @Pointcut("within(by.lobanov.cardmanagementservice.controller.impl.*)")
    public void controllerMethods() {}

    /**
     * Advice @Before: Логгирует информацию о запросе ПЕРЕД выполнением метода контроллера.
     * Используем pointcut requestMappingMethods() для точности.
     */
    @Before("controllerMethods()")
    public void logBeforeMethodExecution(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String methodName = joinPoint.getSignature().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String httpMethod = request.getMethod();
            String requestURI = request.getRequestURI();
            String remoteAddr = request.getRemoteAddr();
            String principalName = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Anonymous";

            String argsString = Arrays.stream(joinPoint.getArgs())
                    .filter(arg -> !(arg instanceof HttpServletRequest ||
                            arg instanceof HttpServletResponse ||
                            arg instanceof org.springframework.security.core.Authentication ||
                            arg instanceof org.springframework.ui.Model ||
                            arg instanceof org.springframework.validation.BindingResult ||
                            arg instanceof org.springframework.data.domain.Pageable
                    ))
                    .map(this::safeToJson)
                    .collect(Collectors.joining(", "));

            log.info(">>> REQUEST [{}] {} {} from [{}] user=[{}], Args=[{}]",
                    httpMethod, requestURI, className + "." + methodName, remoteAddr, principalName, argsString);
        }
    }


    /**
     * Advice @Around: Оборачивает выполнение метода контроллера для логгирования
     * результата (ответа) или исключения ПОСЛЕ его выполнения.
     * Используем pointcut requestMappingMethods() для точности.
     */
    @Around("controllerMethods()")
    public Object logAroundMethodExecution(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exceptionThrown = null;

        try {
            result = proceedingJoinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exceptionThrown = e;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String methodName = proceedingJoinPoint.getSignature().getName();
            String className = proceedingJoinPoint.getTarget().getClass().getSimpleName();

            if (exceptionThrown != null) {
                log.error("<<< ERROR [{}ms] {}.{}() Exception: {} - {}",
                        duration, className, methodName, exceptionThrown.getClass().getSimpleName(), exceptionThrown.getMessage());
            } else {
                String responseBody = getResponseBody(result);
                log.info("<<< RESPONSE [{}ms] {}.{}() Response: {}",
                        duration, className, methodName, responseBody);
            }
        }
    }

    private String safeToJson(Object object) {
        if (object == null) {
            return "null";
        }
        try {
            if (object instanceof org.springframework.http.HttpEntity ||
                    object instanceof org.springframework.web.multipart.MultipartFile ||
                    object instanceof byte[]) {
                return object.getClass().getSimpleName() + "[skipped]";
            }
            if (object instanceof org.springframework.data.domain.Pageable pageable) {
                return String.format("Pageable[page=%d, size=%d, sort=%s]",
                        pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
            }

            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to JSON for logging: {}", e.getMessage());
            return object.toString() + "[Serialization Error]";
        } catch (Exception e) {
            log.warn("Unexpected error during object serialization for logging: {}", e.getMessage());
            return object.toString() + "[Unexpected Serialization Error]";
        }
    }

    private String getResponseBody(Object result) {
        if (result instanceof ResponseEntity<?> responseEntity) {
            Object body = responseEntity.getBody();

            if (body == null) {
                return "[Status: " + responseEntity.getStatusCode() + ", Body: Empty]";
            }
            if (body instanceof PagedResponse<?> pagedResponse) {
                return String.format("PagedResponse[page=%d, size=%d, totalElements=%d, totalPages=%d, contentSize=%d]",
                        pagedResponse.getPageNumber(),
                        pagedResponse.getPageSize(),
                        pagedResponse.getTotalElements(),
                        pagedResponse.getTotalPages(),
                        pagedResponse.getContent() != null ? pagedResponse.getContent().size() : 0);
            }
            return "[Status: " + responseEntity.getStatusCode() + ", Body: " + safeToJson(body) + "]";
        } else {
            return safeToJson(result);
        }
    }
}