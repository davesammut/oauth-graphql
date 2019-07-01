package com.ho.integration.prototype.integration.controllers;

import com.ho.integration.prototype.integration.config.SecurityContextUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.root}")
public class SampleRestController {

//    public static final String usernameKey = "api.username.get";

    @GetMapping(path = "${api.username.get}")
    @PreAuthorize(value = "@RBAC.isAuthorised(\"api.username.get\", #requestWrapper)")
    public ResponseEntity<String> getAuthorizedUserName(SecurityContextHolderAwareRequestWrapper requestWrapper) {
        return ResponseEntity.ok(SecurityContextUtils.getUserName());
    }

//    public static final String forbiddenKey = "api.forbidden.get";

    @GetMapping(path = "${api.forbidden.get}")
    @PreAuthorize(value = "@RBAC.isAuthorised(\"api.forbidden.get\", #requestWrapper)")
    public ResponseEntity<String> getForbidden(SecurityContextHolderAwareRequestWrapper requestWrapper) {
        return ResponseEntity.ok(SecurityContextUtils.getUserName());
    }
}
