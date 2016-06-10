package com.server.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.scholastic.intl.ads.server.exception.ApplicationException;

public class AuthenticationProvider {
	/**
     * The <code>Log</code> instance for this class.
     */
    private static final Log log = LogFactory.getLog(AuthenticationProvider.class);
    private String passwordParameter = "j_password";
    private String usernameParameter= "j_username";
	public AuthenticationToken authenticate(HttpServletRequest req ,	HttpServletResponse res) throws ApplicationException {

		// Get application context
		// Get principal associated with request
		String username = obtainUsername(req);
		String password = obtainPassword(req);
		AuthenticationToken token = null;
		System.out.println("username:" + username + "password:" + password);

		try {
			// Create a xenos principal from the available principal
			if (log.isTraceEnabled()) {
				log.trace("creating custom principal for first time usage");
			}
			token = new AuthenticationToken(username,password);
			
			if(!"admin".equals(username) && !"admin".equals(password))
				throw new ApplicationException();

		} catch (ApplicationException e) {
			// Failed to create the user principal
			log.fatal("Failed to create the user principal", e);
			// handle authentication failure
			throw new ApplicationException("Failed to authenticate");
		}
		
		return token;

	}
	
	
	/**
     * 
     * @param request
     * @return
     */
    protected String obtainPassword(HttpServletRequest request) {
        return request.getParameter(passwordParameter);
    }

    /**
     * Enables subclasses to override the composition of the username, such as by including additional values
     * and a separator.
     *
     * @param request so that request attributes can be retrieved
     *
     * @return the username that will be presented in the <code>Authentication</code> request token to the
     *         <code>AuthenticationManager</code>
     */
    protected String obtainUsername(HttpServletRequest request) {
        return request.getParameter(usernameParameter);
    }

}
