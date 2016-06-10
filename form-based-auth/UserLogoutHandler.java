package com.server.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class UserLogoutHandler  {
	protected final Log logger = LogFactory.getLog(this.getClass());
	private String filterProcessesUrl = "/j_security_logout";
	private String targetUrl = "/#/Login";
	private String cookiesToClear[] = {};
	private DefaultRedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
	
	public void logout(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        if (requiresLogout(request, response)) {
        	
        	// clear session data
        	HttpSession session = request.getSession(false);
            if (session != null) {
                logger.debug("Invalidating session: " + session.getId());
                session.invalidate();
            }
            
        	/*clearing  cookie
             * A logout handler which clears a defined list of cookies, using the context path as the
             * cookie path.
             */
            for (String cookieName : cookiesToClear) {
                Cookie cookie = new Cookie(cookieName, null);
                cookie.setPath(request.getContextPath());
                cookie.setMaxAge(0);
                response.addCookie(cookie);
            }
            
            // redirect to the target url
            redirectStrategy.sendRedirect(request, response, targetUrl);
            
        	
        }
    }
	
	
	/**
     * Allow subclasses to modify when a logout should take place.
     *
     * @param request the request
     * @param response the response
     *
     * @return <code>true</code> if logout should occur, <code>false</code> otherwise
     */
    protected boolean requiresLogout(HttpServletRequest request, HttpServletResponse response) {
        String uri = request.getRequestURI();
        int pathParamIndex = uri.indexOf(';');

        if (pathParamIndex > 0) {
            // strip everything from the first semi-colon
            uri = uri.substring(0, pathParamIndex);
        }

        int queryParamIndex = uri.indexOf('?');

        if (queryParamIndex > 0) {
            // strip everything from the first question mark
            uri = uri.substring(0, queryParamIndex);
        }

        if ("".equals(request.getContextPath())) {
            return uri.endsWith(filterProcessesUrl);
        }

        return uri.endsWith(request.getContextPath() + filterProcessesUrl);
    }

	
}
