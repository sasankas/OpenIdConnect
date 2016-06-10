package com.server.security;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class DefaultRedirectStrategy {
	protected final Log logger = LogFactory.getLog(DefaultRedirectStrategy.class);
	private String defaultTargetUrl = "/";
	private boolean contextRelative;
	
	public void sendRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
		sendRedirect(request, response, defaultTargetUrl);
	}


	/**
	 * Redirects the response to the supplied URL.
	 * <p>
	 * If <tt>contextRelative</tt> is set, the redirect value will be the value after the request context path. Note
	 * that this will result in the loss of protocol information (HTTP or HTTPS), so will cause problems if a
	 * redirect is being performed to change to HTTPS, for example.
	 */
	public void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
		String redirectUrl = calculateRedirectUrl(request.getContextPath(), url);
		redirectUrl = response.encodeRedirectURL(redirectUrl);

		if (logger.isDebugEnabled()) {
			logger.debug("Redirecting to '" + redirectUrl + "'");
		}

		response.sendRedirect(redirectUrl);
	}

	private String calculateRedirectUrl(String contextPath, String url) {
		if (!isAbsoluteUrl(url)) {
			if (contextRelative) {
				return url;
			} else {
				return contextPath + url;
			}
		}

		// Full URL, including http(s)://

		if (!contextRelative) {
			return url;
		}

		// Calculate the relative URL from the fully qualified URL, minus the scheme and base context.
		url = url.substring(url.indexOf("://") + 3); // strip off scheme
		url = url.substring(url.indexOf(contextPath) + contextPath.length());

		if (url.length() > 1 && url.charAt(0) == '/') {
			url = url.substring(1);
		}

		return url;
	}

	/**
	 * If <tt>true</tt>, causes any redirection URLs to be calculated minus the protocol
	 * and context path (defaults to <tt>false</tt>).
	 */
	public void setContextRelative(boolean useRelativeContext) {
		this.contextRelative = useRelativeContext;
	}
	
	/**
     * Decides if a URL is absolute based on whether it contains a valid scheme name, as defined in RFC 1738.
     */
    public static boolean isAbsoluteUrl(String url) {
        final Pattern ABSOLUTE_URL = Pattern.compile("\\A[a-z0-9.+-]+://.*", Pattern.CASE_INSENSITIVE);

        return ABSOLUTE_URL.matcher(url).matches();
    }


}
