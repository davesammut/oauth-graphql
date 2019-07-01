package com.ho.integration.prototype.integration.graphql;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;
import graphql.schema.idl.EnumValuesProvider;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;

import javax.servlet.http.HttpServletRequestWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * This is our wiring used to put behaviour to a graphql type.
 */
public class StarWarsWiring {

    /**
     * The context object is passed to each level of a graphql query and in this case it contains
     * the data loader registry.  This allows us to keep our data loaders per request since
     * they cache data and cross request caches are often not what you want.
     */
    public static class Context {

        // Application
        final DataLoaderRegistry dataLoaderRegistry;

        // GQL plumbing
        final Map<String, Object> variables;
        final Authentication authentication;

        public Context(Map<String, Object> variables, Authentication authentication) {
            this.variables  = variables;
            this.authentication = authentication;

            // Application
            this.dataLoaderRegistry = new DataLoaderRegistry();
            dataLoaderRegistry.register("characters", newCharacterDataLoader());
        }

        public DataLoaderRegistry getDataLoaderRegistry() {
            return dataLoaderRegistry;
        }

        public DataLoader<String, Object> getCharacterDataLoader() {
            return dataLoaderRegistry.getDataLoader("characters");
        }

        public Map<String, Object> getVariables() {
            return this.variables;
        }

        public Authentication getAuthentication() {
            return this.authentication;
        }
    }

    private static List<Object> getCharacterDataViaBatchHTTPApi(List<String> keys) {
        return keys.stream().map(StarWarsData::getCharacterData).collect(Collectors.toList());
    }

    // a batch loader function that will be called with N or more keys for batch loading
    private static BatchLoader<String, Object> characterBatchLoader = keys -> {

        //
        // we are using multi threading here.  Imagine if getCharacterDataViaBatchHTTPApi was
        // actually a HTTP call - its not here - but it could be done asynchronously as
        // a batch API call say
        //
        //
        // direct return of values
        //CompletableFuture.completedFuture(getCharacterDataViaBatchHTTPApi(keys))
        //
        // or
        //
        // async supply of values
        return CompletableFuture.supplyAsync(() -> getCharacterDataViaBatchHTTPApi(keys));
    };

    // a data loader for characters that points to the character batch loader
    private static DataLoader<String, Object> newCharacterDataLoader() {
        return new DataLoader<>(characterBatchLoader);
    }

    // we define the normal StarWars data fetchers so we can point them at our data loader
    public static DataFetcher humanDataFetcher = environment -> {
        String id = environment.getArgument("id");
        Context ctx = environment.getContext();
        return ctx.getCharacterDataLoader().load(id);
    };


    public static DataFetcher droidDataFetcher = environment -> {
        String id = environment.getArgument("id");
        Context ctx = environment.getContext();
        return ctx.getCharacterDataLoader().load(id);
//        Map<String, String> headers = (Map<String, String>)ctx.variables.get("HttpHeaders");
//        if(headers.containsKey("Authorization") && headers.get("Authorization").contains("3PO")) {
//            return ctx.getCharacterDataLoader().load(id);
//        }
//        return new Droid("NO ACCESS", "XXXX", new ArrayList<String>(), new ArrayList<Integer>(), "");
    };

    public static DataFetcher heroDataFetcher = environment -> {
        Context ctx = environment.getContext();
        return ctx.getCharacterDataLoader().load("2001"); // R2D2
    };

    public static DataFetcher friendsDataFetcher = environment -> {
        FilmCharacter character = environment.getSource();
        List<String> friendIds = character.getFriends();
        Context ctx = environment.getContext();
        return ctx.getCharacterDataLoader().loadMany(friendIds);
    };

    /**
     * Character in the graphql type system is an Interface and something needs
     * to decide that concrete graphql object type to return
     */
    public static TypeResolver characterTypeResolver = environment -> {
        FilmCharacter character = (FilmCharacter) environment.getObject();
        if (character instanceof Human) {
            return (GraphQLObjectType) environment.getSchema().getType("Human");
        } else {
            return (GraphQLObjectType) environment.getSchema().getType("Droid");
        }
    };

    public static EnumValuesProvider episodeResolver = Episode::valueOf;
}
