package com.github.sheiy.weblog.autoconfigure;

import jakarta.servlet.*;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/** 线程着色 */
@Component
public class SNShader implements Filter, Ordered {

    @Override
    public void doFilter(
            ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        String sn = UUID.randomUUID().toString().replaceAll("-", "");
        // 设置MDC变量，日志里用
        MDC.put("SN", sn);
        servletRequest.setAttribute("SN", sn);
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
