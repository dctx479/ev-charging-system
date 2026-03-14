package com.ev.charging.common;

import com.ev.charging.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 安全修复: 避免向客户端泄露敏感的系统内部信息
 * - 详细错误信息仅记录到日志
 * - 向客户端返回通用错误消息
 * - 记录请求上下文和堆栈信息
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常 (POST/PUT 请求体校验)
     * 安全修复: 返回通用消息，不暴露具体字段名和验证规则
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request) {
        // 记录详细的验证错误到日志
        String validationErrors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("请求参数校验失败 [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), validationErrors);

        // 向客户端返回通用消息
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "请求参数验证失败");
    }

    /**
     * 处理绑定异常
     * 安全修复: 返回通用消息，不暴露具体字段名和验证规则
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleBindException(
            BindException e,
            HttpServletRequest request) {
        // 记录详细的绑定错误到日志
        String bindingErrors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("请求参数绑定失败 [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), bindingErrors);

        // 向客户端返回通用消息
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "请求参数验证失败");
    }

    /**
     * 处理非法参数异常
     * 安全修复: 返回通用消息，不暴露具体的异常信息
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleIllegalArgumentException(
            IllegalArgumentException e,
            HttpServletRequest request) {
        log.warn("非法参数异常 [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "请求参数不合法");
    }

    /**
     * 处理请求体不可读异常（JSON 格式错误等）
     * 安全修复: 不暴露具体的 JSON 解析错误信息
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e,
            HttpServletRequest request) {
        log.warn("HTTP 请求体格式不可读 [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "请求数据格式错误，请检查 JSON 格式");
    }

    /**
     * 处理HTTP方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<String> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request) {
        log.warn("不支持的 HTTP 方法 [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), e.getMethod());
        return Result.error(ResultCode.METHOD_NOT_ALLOWED.getCode(), "不支持的请求方法");
    }

    /**
     * 处理 @Validated 参数校验异常（路径参数/查询参数校验失败）
     * 安全修复: 返回通用消息，不暴露具体的字段名、约束名和验证规则
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request) {
        // 记录详细的约束违反信息到日志
        String violations = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));

        log.warn("参数约束违反 [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), violations);

        // 向客户端返回通用消息，不暴露约束细节
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "请求参数验证失败");
    }

    /**
     * 处理权限不足异常（403）
     * Spring Security @PreAuthorize 拒绝时抛出此异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<String> handleAccessDeniedException(
            AccessDeniedException e,
            HttpServletRequest request) {
        log.warn("权限不足 [{}] {}", request.getMethod(), request.getRequestURI());
        return Result.error(ResultCode.FORBIDDEN.getCode(), "权限不足，无权访问");
    }

    /**
     * 处理认证异常（401）
     * 用户未认证或认证已过期时抛出此异常
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<String> handleAuthenticationException(
            AuthenticationException e,
            HttpServletRequest request) {
        log.warn("认证失败 [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.UNAUTHORIZED.getCode(), "未认证或认证已过期，请重新登录");
    }

    /**
     * 处理资源不存在异常（404）
     * 请求的 URL 路径在 Spring 中找不到对应的映射
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<String> handleNoHandlerFoundException(
            NoHandlerFoundException e,
            HttpServletRequest request) {
        log.warn("资源不存在 [{}] {} -> {}",
                request.getMethod(), e.getRequestURL(), e.getMessage());
        return Result.error(ResultCode.NOT_FOUND.getCode(), "请求的资源不存在");
    }

    /**
     * 处理数据库唯一约束违反异常（409）
     * 如用户名、手机号重复注册等数据完整性异常
     * 安全修复: 不检查异常消息内容（避免暴露 SQL 错误），统一返回通用消息
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<String> handleDataIntegrityViolationException(
            DataIntegrityViolationException e,
            HttpServletRequest request) {
        log.warn("数据完整性约束违反 [{}] {}: {}",
                request.getMethod(), request.getRequestURI(),
                e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage());
        // 返回通用错误信息，不暴露具体的数据库约束信息
        return Result.error(ResultCode.ERROR.getCode(), "数据操作失败，请检查输入是否合法");
    }

    /**
     * 处理乐观锁异常（409）
     * 当多个线程同时修改带 @Version 的实体时抛出此异常
     * 常见场景：并发更新订单、排队记录等带版本号的数据
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<String> handleOptimisticLockingFailureException(
            OptimisticLockingFailureException e,
            HttpServletRequest request) {
        log.warn("乐观锁冲突 [{}] {}: 数据已被其他操作修改，请重试",
                request.getMethod(), request.getRequestURI());
        // 返回业务异常，提示客户端重试
        return Result.error(ResultCode.ERROR.getCode(), "操作冲突，请重新刷新后重试");
    }

    /**
     * 处理实体未找到异常（404）
     * JPA 在使用 getOne() 或类似方法时，如果实体不存在会抛出此异常
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<String> handleEntityNotFoundException(
            EntityNotFoundException e,
            HttpServletRequest request) {
        log.warn("实体不存在 [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage());
        // 返回通用错误信息，不暴露具体的实体类型和查询条件
        return Result.error(ResultCode.NOT_FOUND.getCode(), "请求的资源不存在");
    }

    /**
     * 处理空结果数据访问异常（404）
     * 当查询结果为空但代码期望有结果时抛出此异常
     */
    @ExceptionHandler(EmptyResultDataAccessException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<String> handleEmptyResultDataAccessException(
            EmptyResultDataAccessException e,
            HttpServletRequest request) {
        log.warn("查询结果为空 [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage());
        // 返回通用错误信息，避免暴露数据库查询细节
        return Result.error(ResultCode.NOT_FOUND.getCode(), "请求的资源不存在");
    }

    /**
     * 处理缺少必要请求参数异常（400）
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e,
            HttpServletRequest request) {
        log.warn("缺少必要的请求参数 [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), e.getParameterName());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "请求参数不完整");
    }

    /**
     * 处理不支持的媒体类型异常（415）
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<String> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e,
            HttpServletRequest request) {
        log.warn("不支持的媒体类型 [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), e.getContentType());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "请求内容类型不被支持");
    }

    /**
     * 处理文件上传异常（400）
     */
    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleMultipartException(
            MultipartException e,
            HttpServletRequest request) {
        log.warn("文件上传异常 [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), "文件上传失败，请检查文件大小和格式");
    }

    /**
     * 处理业务异常（返回具体业务错误信息）
     * BusinessException extends RuntimeException，必须在通用RuntimeException处理器之前声明
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<String> handleBusinessException(
            BusinessException e,
            HttpServletRequest request) {
        log.warn("业务异常 [{}] {}: code={}, message={}",
                request.getMethod(), request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理运行时异常
     * 安全修复: 只记录到日志，不向客户端返回具体错误信息，避免暴露堆栈跟踪和内部细节
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> handleRuntimeException(
            RuntimeException e,
            HttpServletRequest request) {
        log.error("运行时异常 [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage(), e);
        // 只返回通用错误信息，不暴露具体异常详情、堆栈信息或内部实现细节
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR.getCode(),
                "服务器错误，请稍后重试");
    }

    /**
     * 处理其他异常（通用异常处理，应该是最后一个处理器）
     * 安全修复: 只记录到日志，不向客户端返回具体错误信息，避免暴露堆栈跟踪和内部细节
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> handleException(
            Exception e,
            HttpServletRequest request) {
        log.error("未预期的异常 [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage(), e);
        // 只返回通用错误信息，不暴露系统内部细节、堆栈跟踪或异常类型
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR.getCode(),
                "系统异常，请联系管理员");
    }
}
