package com.server.security;

import java.util.Collection;


import com.scholastic.intl.ads.server.exception.ApplicationException;

public class AuthenticationToken {
	
	private String principal; 
	private String credentials;
	private boolean authenticated;

	public AuthenticationToken(String principal, String credentials) throws ApplicationException{
		this.principal = principal;
		this.credentials = credentials;
	}

	public boolean isAuthenticated() {
		return authenticated;
	}

	public void setAuthenticated(boolean authenticated) {
		this.authenticated = authenticated;
	}

	public String getPrincipal() {
		return principal;
	}

	public String getCredentials() {
		return credentials;
	}

	
	 
	 
}
