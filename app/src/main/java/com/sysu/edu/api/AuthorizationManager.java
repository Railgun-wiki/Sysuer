package com.sysu.edu.api;

import java.util.regex.Pattern;

public class AuthorizationManager {

    private final String origin;
    private final String substitute;
    boolean isAuthorized = true;
    boolean isAccessible = true;

    public AuthorizationManager(String origin, String substitute) {
        this.origin = origin;
        this.substitute = substitute;
    }

    public String getOrigin() {
        return origin;
    }

    public String getBaseUrl() {
        return isAccessible ? origin : substitute;
    }

    public boolean isAccessible(String content) {
        isAccessible = !Pattern.compile("Access Forbidden").matcher(content).find();
        return isAccessible;
    }

    public boolean isAccessible() {
        return isAccessible;
    }

    public void setAccessible(boolean accessible) {
        isAccessible = accessible;
    }

    public boolean isAuthorized() {
        return isAuthorized;
    }

    public boolean isAuthorized(String content) {
        isAuthorized = !Pattern.compile("中山大学统一身份认证").matcher(content).find();
        return isAuthorized;
    }
}
