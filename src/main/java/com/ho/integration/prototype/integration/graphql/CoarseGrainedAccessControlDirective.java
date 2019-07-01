package com.ho.integration.prototype.integration.graphql;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;

import java.util.Map;

public class CoarseGrainedAccessControlDirective implements SchemaDirectiveWiring {
    @Override
    public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> env) {
        String targetAuthRole = (String) env.getDirective().getArgument("role").getValue();
        SimpleGrantedAuthority requiredAuthority = new SimpleGrantedAuthority(targetAuthRole);

        GraphQLFieldDefinition field = env.getElement();
        GraphQLFieldsContainer parentType = env.getFieldsContainer();

        DataFetcher originalDataFetcher = env.getCodeRegistry().getDataFetcher(parentType, field);
        DataFetcher authDataFetcher = dataFetchingEnvironment -> {
            StarWarsWiring.Context ctx = dataFetchingEnvironment.getContext();
            Authentication authentication = ctx.getAuthentication();
            if(authentication.getAuthorities().contains(requiredAuthority)) {
                return originalDataFetcher.get(dataFetchingEnvironment);
            }
            return "Redacted Content! You can't see this data because you must have role [" + targetAuthRole + "]";
        };
        env.getCodeRegistry().dataFetcher(parentType, field, authDataFetcher);
        return field;
    }
}