package com.photowebsite.controller;

import com.photowebsite.service.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private DataStore dataStore;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> credentials) {
        Map<String, Object> response = new HashMap<>();
        String username = credentials.get("username");
        String password = credentials.get("password");

        Map<String, String> user = dataStore.findUser(username);
        if (user != null && user.get("password").equals(password)) {
            response.put("success", true);
            response.put("token", user.get("token"));
            response.put("username", username);
            response.put("displayName", user.get("displayName"));
            response.put("role", user.get("role"));
        } else {
            response.put("success", false);
            response.put("message", "Invalid username or password");
        }
        return response;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) throws IOException {
        Map<String, Object> response = new HashMap<>();
        String username = body.get("username");
        String password = body.get("password");
        String displayName = body.get("displayName");

        if (username == null || username.trim().isEmpty() || password == null || password.length() < 4) {
            response.put("success", false);
            response.put("message", "Username required and password must be at least 4 characters");
            return response;
        }

        boolean ok = dataStore.registerUser(username.trim(), password, displayName);
        if (!ok) {
            response.put("success", false);
            response.put("message", "Username already exists");
            return response;
        }

        Map<String, String> user = dataStore.findUser(username.trim());
        response.put("success", true);
        response.put("token", user.get("token"));
        response.put("username", username.trim());
        response.put("displayName", user.get("displayName"));
        response.put("role", user.get("role"));
        return response;
    }

    @GetMapping("/me")
    public Map<String, Object> me(@RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> user = dataStore.findUserByToken(token);
        if (user == null) {
            response.put("valid", false);
        } else {
            response.put("valid", true);
            response.put("role", user.get("role"));
            response.put("displayName", user.get("displayName"));
        }
        return response;
    }
}
