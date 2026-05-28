package com.easyschedule.backend.auth.dto.request;

public class GoogleLoginRequest {

    private String credential;

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }
}