package com.ho.integration.prototype.integration.graphql;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;

import java.util.Map;

public class ExperimentDirective implements SchemaDirectiveWiring {
    @Override
    public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> env) {

        GraphQLFieldDefinition field = env.getElement();
//        GraphQLFieldsContainer parentType = env.getFieldsContainer();
//        field.getArgument("role");
//        //
//        // build a data fetcher that first checks authorisation roles before then calling the original data fetcher
//        //
//        DataFetcher originalDataFetcher = env.getCodeRegistry().getDataFetcher(parentType, field);
//        DataFetcher authDataFetcher = dataFetchingEnvironment -> {
//            StarWarsWiring.Context ctx = dataFetchingEnvironment.getContext();
//
//            boolean isRoleMatched = supportedRoles.stream().anyMatch(ctx.getRequestWrapper()::isUserInRole);
//
//            Map<String, String> headers = (Map<String, String>) ctx.variables.get("HttpHeaders");
//            if (headers.containsKey("Authorization") && headers.get("Authorization").contains("JWT")) {
//                try {
//                    return originalDataFetcher.get(dataFetchingEnvironment);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    return "XXXX - Coarse grained access control";
//                }
//            }
//            return "YYYY - Coarse grained access control";
//        };
//        //
//        env.getCodeRegistry().dataFetcher(parentType, field, authDataFetcher);
        return field;
    }
}