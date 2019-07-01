package com.ho.integration.prototype.integration.graphql;

import java.util.List;

public interface FilmCharacter {
    String getId();

    String getName();

    List<String> getFriends();

    List<Integer> getAppearsIn();
}
