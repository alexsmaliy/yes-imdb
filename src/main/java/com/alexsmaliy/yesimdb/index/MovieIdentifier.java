package com.alexsmaliy.yesimdb.index;

import org.immutables.value.Value;

@Value.Immutable
public interface MovieIdentifier {
    String title();

    String url();

    @Value.Lazy
    default String fullCreditsUrl() {
        return url().replaceFirst("/\\?", "/fullcredits/?");
    }
}
