package com.ho.integration.prototype.integration.graphql;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Graphql clients can send GET or POST HTTP requests.  The spec does not make an explicit
 * distinction.  So you may need to handle both.  The following was tested using
 * a graphiql client tool found here : https://github.com/skevy/graphiql-app
 *
 * You should consider bundling graphiql in your application
 *
 * https://github.com/graphql/graphiql
 *
 * This outlines more information on how to handle parameters over http
 *
 * http://graphql.org/learn/serving-over-http/
 */
public class QueryParameters {

    String query;
    String operationName;
    Map<String, Object> variables = Collections.emptyMap();

    public String getQuery() {
        return query;
    }

    public String getOperationName() {
        return operationName;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public static QueryParameters from(HttpServletRequest request) {
        QueryParameters parameters = new QueryParameters();
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            Map<String, Object> json = readJSON(request);
            parameters.query = (String) json.get("query");
            parameters.operationName = (String) json.get("operationName");
            parameters.variables = getVariables(json.get("variables"), request);
        } else {
            parameters.query = request.getParameter("query");
            parameters.operationName = request.getParameter("operationName");
            parameters.variables = getVariables(request.getParameter("variables"), request);
        }
        return parameters;
    }

    private static Map<String, Object> getHeaders(HttpServletRequest httpRequest) {
        Map<String, Object> headers = new HashMap<String, Object>();
        Enumeration headerNames = httpRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = httpRequest.getHeader(key);
            headers.put(key, value);
        }
        return headers;
    }


    private static Map<String, Object> getVariables(Object variables, HttpServletRequest httpRequest) {
        if (variables instanceof Map) {
            Map<?, ?> inputVars = (Map) variables;
            Map<String, Object> vars = new HashMap<>();
            inputVars.forEach((k, v) -> vars.put(String.valueOf(k), v));
            vars.put("HttpHeaders", getHeaders(httpRequest));
            return vars;
        }
        Map<String, Object> parsedVariables = JsonKit.toMap(String.valueOf(variables));
        Map<String, Object> variablesAndHeaders = new HashMap<>(parsedVariables);
        variablesAndHeaders.put("HttpHeaders", getHeaders(httpRequest));
        return variablesAndHeaders;
    }

    private static Map<String, Object> readJSON(HttpServletRequest request) {
        String s = readPostBody(request);
        return JsonKit.toMap(s);
    }

    private static String readPostBody(HttpServletRequest request) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            int c;
            while ((c = reader.read()) != -1) {
                sb.append((char) c);
            }
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
