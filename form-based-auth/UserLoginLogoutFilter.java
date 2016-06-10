package com.server.security;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserLoginLogoutFilter implements Filter {
	
	private UserLogoutHandler logoutHandler = new UserLogoutHandler();
	private UserLoginHandler  loginHandler = new UserLoginHandler();
	
	
	public void login(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
		
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

    	HttpServletRequest req = (HttpServletRequest) request;
    	HttpServletResponse res = (HttpServletResponse) response;

    	System.out.println("*******************************************");
		if (logoutHandler.requiresLogout(req, res) ){
			logoutHandler.logout(req, res);
		} else {
			loginHandler.login(req, res, chain);
		}
				
	}

	@Override
	public void destroy() {
		
	}
}
