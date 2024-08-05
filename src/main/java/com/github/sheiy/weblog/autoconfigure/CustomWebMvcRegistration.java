package com.github.sheiy.weblog.autoconfigure;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

/** HTTP日志 */
@Configuration
@AutoConfigureBefore(ErrorMvcAutoConfiguration.class)
public class CustomWebMvcRegistration implements WebMvcRegistrations {

    private static final Long FIVE_SECONDS = 5 * 1000L;

    private static final Long MAX_LENGTH = 1024L;

    private static final String NULL = "null";

    private static final String SQUARE_BRACKETS = "[]";

    private static final String LEFT_SQUARE_BRACKET = "[";

    private static final String RIGHT_SQUARE_BRACKET = "]";

    private static final String SERVLET_REQUEST = "ServletRequest";

    private static final String SERVLET_RESPONSE = "ServletResponse";

    private static final String COMMA = ",";

    private static final String LEFT_BRACKET = "(";

    private static final String RIGHT_BRACKET = ")";

    private static final String FILE_START = "File" + LEFT_BRACKET;

    private static final String ORIGINAL_FILE_NAME = "originalFilename";

    private static final String NAME = "name";

    private static final String CONTENT_TYPE = "contentType";

    private static final String SIZE = "size";

    private static final String EQUAL = "=";

    private static final String UNKNOWN = "unknown";

    private static final String TOO_LONG_WITH_BRACKETS = "(too long)";

    private static final String TOO_LONG_WITH_SQUARE_BRACKET = "[too long]";

    private static final String REQUEST_BIND = "HTTP请求处理开始:[handler={},ip={},request={}]";

    private static final String SLOW_BIND = "发现HTTP请求处理过慢接口:[handler={},处理耗时:{}毫秒]";

    private static final String RESPONSE_BIND = "HTTP请求处理结束:[handler={},ip={},response={}]";

    private static final String ERROR_BIND = "HTTP请求处理失败:[path=%s,handler=%s,ip=%s], %s";

    private static final String SPRINGFOX = "springfox";

    @Bean
    public ErrorAttributes errorAttributes() {
        return new DefaultErrorAttributes() {
            @Override
            public Map<String, Object> getErrorAttributes(
                    WebRequest webRequest, ErrorAttributeOptions options) {
                Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);
                errorAttributes.put("sn", webRequest.getAttribute("SN", RequestAttributes.SCOPE_REQUEST));
                LoggerFactory.getLogger(DefaultErrorAttributes.class).error("出现跳转错误页");
                return errorAttributes;
            }
        };
    }

    @Bean
    Filter shShader() {
        return new SNShader();
    }

    @Override
    public RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
        return new RequestMappingHandlerAdapter() {
            @NonNull
            @Override
            protected ServletInvocableHandlerMethod createInvocableHandlerMethod(
                    @NonNull HandlerMethod handlerMethod) {
                return new ServletInvocableHandlerMethod(handlerMethod) {
                    @Override
                    public Object invokeForRequest(
                            @NonNull NativeWebRequest request,
                            ModelAndViewContainer mavContainer,
                            @NonNull Object... providedArgs)
                            throws Exception {
                        // 处理HTTP请求的方法
                        String handler = ClassUtils.getQualifiedMethodName(getMethod(), getBeanType());
                        request.setAttribute("Handler", handler, RequestAttributes.SCOPE_REQUEST);
                        // 发送请求的IP
                        String ip = getIpAddr(request);
                        request.setAttribute("IP", ip, RequestAttributes.SCOPE_REQUEST);
                        // 请求参数
                        Object[] args = getMethodArgumentValues(request, mavContainer, providedArgs);
                        Logger log = LoggerFactory.getLogger(getMethod().getDeclaringClass());
                        // 启用了日志并且不是Swagger的请求才打印日志
                        boolean logDebug = log.isDebugEnabled() && !handler.contains(SPRINGFOX);
                        if (logDebug) {
                            Object userDetail =
                                    request.getAttribute("UserDetail", RequestAttributes.SCOPE_REQUEST);
                            /* 请求入参日志 */
                            String requestParam = argsToString(getMethod().getParameterAnnotations(), args);
                            log.debug(REQUEST_BIND, handler, ip, requestParam);
                            if (userDetail != null) {
                                log.debug("usersDetail:{}", userDetail);
                            }
                            // 开始处理时间
                            long startTime = System.currentTimeMillis();
                            Object returnValue = doInvoke(args);
                            // 处理请求花费时间
                            long cost = System.currentTimeMillis() - startTime;
                            // 慢接口日志
                            if (cost > FIVE_SECONDS) {
                                log.warn(SLOW_BIND, handler, cost);
                            }
                            String response = NULL;
                            if (returnValue != null) {
                                response = returnValue.toString();
                            }
                            String responseString =
                                    response.length() > MAX_LENGTH ? TOO_LONG_WITH_SQUARE_BRACKET : response;
                            log.debug(
                                    RESPONSE_BIND,
                                    handler,
                                    ip,
                                    returnValue == null
                                            ? NULL
                                            : returnValue.getClass().getSimpleName()
                                                    + LEFT_BRACKET
                                                    + responseString
                                                    + RIGHT_BRACKET);
                            return returnValue;
                        } else {
                            return doInvoke(args);
                        }
                    }
                };
            }
        };
    }

    @Override
    public ExceptionHandlerExceptionResolver getExceptionHandlerExceptionResolver() {
        final ExceptionHandlerExceptionResolver resolver =
                new ExceptionHandlerExceptionResolver() {
                    @NonNull
                    @Override
                    protected String buildLogMessage(
                            @NonNull Exception ex, @NonNull HttpServletRequest request) {
                        Object handler = request.getAttribute("Handler");
                        String path = request.getRequestURI();
                        Object ip = request.getAttribute("IP");
                        return String.format(ERROR_BIND, path, handler, ip, super.buildLogMessage(ex, request));
                    }
                };
        resolver.setWarnLogCategory(CustomWebMvcRegistration.class.getName());
        return resolver;
    }

    public String getIpAddr(NativeWebRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            if (request instanceof ServletWebRequest) {
                HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
                ip = servletRequest.getRemoteAddr();
            } else {
                ip = UNKNOWN;
            }
        }
        if (!StringUtils.hasText(ip)) {
            ip = UNKNOWN;
        }
        return ip;
    }

    private String argsToString(Annotation[][] parameterAnnotations, Object[] args) {
        if (args == null) {
            return NULL;
        }

        if (args.length <= 0) {
            return SQUARE_BRACKETS;
        }

        StringJoiner stringJoiner = new StringJoiner(COMMA, LEFT_SQUARE_BRACKET, RIGHT_SQUARE_BRACKET);
        out:
        for (int i = 0; i < args.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof AuthenticationPrincipal) {
                    continue out;
                }
            }
            Object arg = args[i];
            if (arg == null) {
                stringJoiner.add(NULL);
            } else if (arg instanceof ServletRequest) {
                stringJoiner.add(SERVLET_REQUEST);
            } else if (arg instanceof ServletResponse) {
                stringJoiner.add(SERVLET_RESPONSE);
            } else if (arg instanceof MultipartFile) {
                MultipartFile file = (MultipartFile) arg;
                stringJoiner.add(multipartFileToString(file));
            } else {
                String argString = arg.toString();
                if (argString.length() > MAX_LENGTH) {
                    stringJoiner.add(arg.getClass().getSimpleName()).add(TOO_LONG_WITH_BRACKETS);
                } else {
                    stringJoiner.add(argString);
                }
            }
        }
        return stringJoiner.toString();
    }

    private String multipartFileToString(MultipartFile file) {
        return FILE_START
                + NAME
                + EQUAL
                + file.getName()
                + COMMA
                + ORIGINAL_FILE_NAME
                + EQUAL
                + file.getOriginalFilename()
                + COMMA
                + CONTENT_TYPE
                + EQUAL
                + file.getContentType()
                + COMMA
                + SIZE
                + EQUAL
                + file.getSize()
                + RIGHT_BRACKET;
    }
}
