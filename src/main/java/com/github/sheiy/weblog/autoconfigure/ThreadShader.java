package com.github.sheiy.weblog.autoconfigure;

import java.io.IOException;
import java.util.UUID;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * 线程着色
 */
@Component
public class ThreadShader implements Filter, Ordered {

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		String sn = UUID.randomUUID().toString().replaceAll("-", "");
		servletRequest.setAttribute("SN", sn);
		String originalThreadName = Thread.currentThread().getName();
		Thread.currentThread().setName(sn);
		try {
			filterChain.doFilter(servletRequest, servletResponse);
		}
		finally {
			Thread.currentThread().setName(originalThreadName);
		}
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

}
