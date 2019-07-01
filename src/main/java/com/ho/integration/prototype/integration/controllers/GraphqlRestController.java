package com.ho.integration.prototype.integration.controllers;

import com.ho.integration.prototype.integration.config.SecurityContextUtils;
import com.ho.integration.prototype.integration.graphql.CoarseGrainedAccessControlDirective;
import com.ho.integration.prototype.integration.graphql.JsonKit;
import com.ho.integration.prototype.integration.graphql.QueryParameters;
import com.ho.integration.prototype.integration.graphql.StarWarsWiring;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.execution.instrumentation.tracing.TracingInstrumentation;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.dataloader.DataLoaderRegistry;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

import static graphql.ExecutionInput.newExecutionInput;
import static graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions.newOptions;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;
import static java.util.Arrays.asList;

@RestController
@RequestMapping("/")
public class GraphqlRestController {

    static GraphQLSchema starWarsSchema = null;


    @RequestMapping(value = "${api.graphql.post}", method = RequestMethod.POST)
    @PreAuthorize(value = "@RBAC.isAuthorised(\"api.graphql.post\", #requestWrapper)")
    public void message(Authentication authentication, SecurityContextHolderAwareRequestWrapper requestWrapper, HttpServletRequest request, HttpServletResponse response) throws IOException {
        handleStarWars(request, response, authentication);
    }

    private void handleStarWars(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Authentication authentication) throws IOException {

        // GQL Plumbing
        QueryParameters parameters = QueryParameters.from(httpRequest);
        if (parameters.getQuery() == null) {
            httpResponse.setStatus(400);
            return;
        }

        // GQL Plumbing & Application TODO: separate concerns
        StarWarsWiring.Context context = new StarWarsWiring.Context(parameters.getVariables(), authentication);
        DataLoaderRegistry dataLoaderRegistry = context.getDataLoaderRegistry();

        // GQL Plumbing
        ExecutionInput.Builder executionInput = newExecutionInput()
                .query(parameters.getQuery())
                .operationName(parameters.getOperationName())
                .dataLoaderRegistry(dataLoaderRegistry)
                .variables(parameters.getVariables());

        // GQL Plumbing
        executionInput.context(context);

        // GQL Plumbing
        DataLoaderDispatcherInstrumentation dlInstrumentation =
                new DataLoaderDispatcherInstrumentation(newOptions().includeStatistics(true));

        Instrumentation instrumentation = new ChainedInstrumentation(
                asList(new TracingInstrumentation(), dlInstrumentation));

        // Application
        GraphQLSchema schema = buildStarWarsSchema();

        // GQL Plumbing
        GraphQL graphQL = GraphQL
                .newGraphQL(schema)
                // instrumentation is pluggable
                .instrumentation(instrumentation)
                .build();

        // GQL Plumbing
        ExecutionResult executionResult = graphQL.execute(executionInput.build());

        // GQL Plumbing
        returnAsJson(httpResponse, executionResult);
    }


    private void returnAsJson(HttpServletResponse response, ExecutionResult executionResult) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        JsonKit.toJson(response, executionResult.toSpecification());
    }

    private GraphQLSchema buildStarWarsSchema() throws IOException {
        //
        // using lazy loading here ensure we can debug the schema generation
        // and potentially get "wired" components that cant be accessed
        // statically.
        //
        // A full application would use a dependency injection framework (like Spring)
        // to manage that lifecycle.
        //
        if (starWarsSchema == null) {

            // Configuration based schema
            Reader streamReader = loadSchemaFile("starWarsSchemaAnnotated.graphqls");
            TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(streamReader);

            // Necessary duplication of Schema file and data fetcher mapping
            // Unmapped fields simply return null
            RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
                    .type(newTypeWiring("Query")
                            .dataFetcher("hero", StarWarsWiring.heroDataFetcher)
                            .dataFetcher("human", StarWarsWiring.humanDataFetcher)
                            .dataFetcher("droid", StarWarsWiring.droidDataFetcher)
                    )
                    .type(newTypeWiring("Human")
                            .dataFetcher("friends", StarWarsWiring.friendsDataFetcher)
                    )
                    .type(newTypeWiring("Droid")
                            .dataFetcher("friends", StarWarsWiring.friendsDataFetcher)
                    )

                    .type(newTypeWiring("Character")
                            .typeResolver(StarWarsWiring.characterTypeResolver)
                    )
                    .type(newTypeWiring("Episode")
                            .enumValues(StarWarsWiring.episodeResolver)
                    ).directive("auth", new CoarseGrainedAccessControlDirective()) // GQL Plumbing
                    .build();

            // finally combine the logical schema with the physical runtime
            starWarsSchema = new SchemaGenerator().makeExecutableSchema(typeRegistry, wiring);
        }
        return starWarsSchema;
    }

    // GQL Plumbing - configuration driven
    @SuppressWarnings("SameParameterValue")
    private Reader loadSchemaFile(String name) throws IOException {
        File schema = new File("src/main/resources/" + name);
        InputStream stream = new FileInputStream(schema);
        return new InputStreamReader(stream);
    }
}
