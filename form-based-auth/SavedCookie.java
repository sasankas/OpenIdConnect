package com.server.security;

import java.io.Serializable;

import javax.servlet.http.Cookie;

public class SavedCookie implements Serializable {
    private final java.lang.String name;
    private final java.lang.String value;
    private final java.lang.String comment;
    private final java.lang.String domain;
    private final int maxAge;
    private final java.lang.String path;
    private final boolean secure;
    private final int version;

    public SavedCookie(String name, String value, String comment, String domain, int maxAge, String path, boolean secure, int version) {
        this.name = name;
        this.value = value;
        this.comment = comment;
        this.domain = domain;
        this.maxAge = maxAge;
        this.path = path;
        this.secure = secure;
        this.version = version;
    }

    public SavedCookie(Cookie cookie) {
        this(cookie.getName(), cookie.getValue(), cookie.getComment(),
                cookie.getDomain(), cookie.getMaxAge(), cookie.getPath(), cookie.getSecure(), cookie.getVersion());
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getComment() {
        return comment;
    }

    public String getDomain() {
        return domain;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public String getPath() {
        return path;
    }

    public boolean isSecure() {
        return secure;
    }

    public int getVersion() {
        return version;
    }

    public Cookie getCookie() {
        Cookie c = new Cookie(getName(), getValue());

        if (getComment() != null)
            c.setComment(getComment());

        if (getDomain() != null)
            c.setDomain(getDomain());

        if (getPath() != null)
            c.setPath(getPath());

        c.setVersion(getVersion());
        c.setMaxAge(getMaxAge());
        c.setSecure(isSecure());
        return c;
    }

}
