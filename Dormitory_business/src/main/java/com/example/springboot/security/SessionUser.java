package com.example.springboot.security;

import java.io.Serializable;

public final class SessionUser implements Serializable {
    private static final long serialVersionUID = 1L;
    public final String role;
    public final String username;
    public final long version;
    public SessionUser(String role, String username, long version) {
        this.role = role; this.username = username; this.version = version;
    }
}
