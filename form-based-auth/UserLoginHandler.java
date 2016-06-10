package com.server.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.scholastic.intl.ads.server.exception.ApplicationException;
public class UserLoginHandler {

    /**
     * The <code>Log</code> instance for this class.
     */
    private static final Log log = LogFactory.getLog(UserLoginHandler.class);


    //~ Instance Attributes ====================================================

    private static final String AUTHENTICATION_EXCEPTION = "AUTHENTICATION_EXCEPTION";
    public static final String USER_KEY = "user_key";
    public static final String SAVED_REQUEST = "SAVED_REQUEST";
    
    private static final Long maxInactivePeriodInSecs = new Long("100"); 
    private String errorPage = "/error.jsp"; 
    private String loginPage = "/#/Login";         
    private String filterProcessesUrl = "j_security_check";
    private AuthenticationProvider authenticationProvider =new AuthenticationProvider();
    private DefaultRedirectStrategy defaultRedirectStrategy = new DefaultRedirectStrategy();
    private PortResolver portResolver = new PortResolver();
    private AntPathRequestMatcher cssRequestMatcher = new AntPathRequestMatcher("/css/**");
    private AntPathRequestMatcher disRequestMatcher = new AntPathRequestMatcher("/distribution/**");
    private AntPathRequestMatcher jsRequestMatcher = new AntPathRequestMatcher("/js/**");
    private AntPathRequestMatcher appRequestMatcher = new AntPathRequestMatcher("/app/**");
    private AntPathRequestMatcher loginPageRequestMatcher = new AntPathRequestMatcher(loginPage);
    private AntPathRequestMatcher errorPageRequestMatcher = new AntPathRequestMatcher(errorPage);
    
    //~ Implemented Methods ====================================================
    
    public void login(HttpServletRequest request,
    		HttpServletResponse response,
                         FilterChain chain)
            throws ServletException,
            IOException {
    	
    	HttpSession session = request.getSession();
    	AuthenticationToken token = (AuthenticationToken) session.getAttribute(USER_KEY);
    	if(token == null && isRestrictedUrl(request)) {
    		// needs to authenticate 
    		if (requiresAuthentication(request, response)) { 
    			try {
    				token = authenticationProvider.authenticate(request, response);
    				successfulAuthentication(request, response, token); 
    			} catch(ApplicationException ex){
    				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication Failed: " + ex.getMessage());
    				request.setAttribute(AUTHENTICATION_EXCEPTION, ex);
    				request.getSession().setAttribute(AUTHENTICATION_EXCEPTION, ex);
    				authorizationFailure(request,response);
    			}
    		} else {
    			//save url and forward the request to login page
    			saveRequest(request, response);
    			request.getRequestDispatcher(loginPage).forward(request, response);
    		}
    	}
    	chain.doFilter(request, response);
    }
    
    public boolean isRestrictedUrl(HttpServletRequest req){
    	return cssRequestMatcher.matches(req)? false:
    		disRequestMatcher.matches(req)? false:
    			jsRequestMatcher.matches(req)? false:
    				appRequestMatcher.matches(req)? false:
    					loginPageRequestMatcher	.matches(req)? false: 
    						errorPageRequestMatcher.matches(req)? false:true;
    }


	/**
     * Handle authorization failure.
     * <p/>
     * Authorization failure means Container has already authenticated
     * the identity of the user, but the user does not have access to this context. However,
     * he may have access to other contexts in the same SSO cloud.
     * <p/>
     * 
     * @param request
     * @param response
     *
     * @throws IOException
     * @throws ServletException
     */
    private void authorizationFailure(ServletRequest request,
                                      ServletResponse response)
            throws ServletException, IOException {

        // Now, this means user is authenticated by the container
        // but he does not have access to this context (per say).
        // Thus, we forward this request to a separate resource
        RequestDispatcher disp
                = request.getRequestDispatcher(errorPage);

        if (disp != null) {
            if (log.isTraceEnabled()) {
                log.trace("dispatching request to enable "
                          + "user switch enterprise");
            }
            disp.forward(request, response);
        } else {
            if (log.isTraceEnabled()) {
                log.trace("request dispatcher is NULL request would be blocked");
            }
        }

    }
    
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, 
    		AuthenticationToken authToken) throws IOException, ServletException {

    	// Bind in the session scope
    	request.getSession().setAttribute(UserLoginHandler.USER_KEY, authToken);
    	if (log.isTraceEnabled()) {
    		log.trace("custom principal bound successfully");
    	}

    	// Set session timeout
    	Long timeout = maxInactivePeriodInSecs;
    	if(timeout != null) {
    		request.getSession().setMaxInactiveInterval(timeout.intValue());
    	}
    	
    	//get saved request
    	DefaultSavedRequest savedRequest = (DefaultSavedRequest) request.getSession().getAttribute(SAVED_REQUEST);
    	
    	if(savedRequest == null)
    		 defaultRedirectStrategy.sendRedirect(request, response);
    	 else 
    		 defaultRedirectStrategy.sendRedirect(request, response,savedRequest.getRedirectUrl());
    	 
    	 //SavedRequest savedRequest = requestCache.getRequest(request, response);
    	//String targetUrl = determineTargetUrl(request, response);
    	clearAuthenticationAttributes(request); 
    }
    
    
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        String uri = request.getRequestURI();
        int pathParamIndex = uri.indexOf(';');

        if (pathParamIndex > 0) {
            // strip everything after the first semi-colon
            uri = uri.substring(0, pathParamIndex);
        }

        if ("".equals(request.getContextPath())) {
            return uri.endsWith(filterProcessesUrl);
        }

        return uri.endsWith(request.getContextPath() + filterProcessesUrl);
    }
    
    /**
     * Removes temporary authentication-related data which may have been stored in the session
     * during the authentication process.
     */
    protected final void clearAuthenticationAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        session.removeAttribute(AUTHENTICATION_EXCEPTION);
    }
    
    
    public void saveRequest(HttpServletRequest request, HttpServletResponse response) {
    	DefaultSavedRequest savedRequest = new DefaultSavedRequest(request, portResolver);

        if (request.getSession(false) != null) {
            // Store the HTTP request itself. Used by AbstractAuthenticationProcessingFilter
            // for redirection after successful authentication (SEC-29)
            request.getSession().setAttribute(SAVED_REQUEST, savedRequest);
            log.debug("DefaultSavedRequest added to Session: " + savedRequest);
        }
    }
    
    

}