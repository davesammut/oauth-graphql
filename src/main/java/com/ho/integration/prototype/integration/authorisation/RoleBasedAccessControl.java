package com.ho.integration.prototype.integration.authorisation;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.Dsl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

@Service(value = "RBAC")
public class RoleBasedAccessControl {

    @Autowired
    private Environment env;

    public Boolean isAuthorised(String supportedRolesKey, SecurityContextHolderAwareRequestWrapper requestWrapper) {
        List<String> supportedRoles = Arrays.asList(env.getProperty(supportedRolesKey + ".roles").split(","));
        boolean isRoleMatched = supportedRoles.stream().anyMatch(requestWrapper::isUserInRole);

        if (!isRoleMatched) {
            try {
                // TODO: Only audit failures or all access?
                audit(requestWrapper);
            } catch (UnsupportedEncodingException e) {
                //TODO: Exception handling strategy
                System.out.println("Failed to send Audit request.");
            }
        }

        return isRoleMatched;
    }

    private void audit(SecurityContextHolderAwareRequestWrapper requestWrapper) throws UnsupportedEncodingException {
        AsyncHttpClient client = Dsl.asyncHttpClient();

        //TODO: What data to audit?
        BoundRequestBuilder getRequest = client.prepareGet(env.getProperty("rbac.audit-uri") + "?userId="
                + requestWrapper.getUserPrincipal().getName()
                + "&resource=" + URLEncoder.encode(requestWrapper.getRequestURI(), "UTF-8"));
        getRequest.execute();
    }
}
