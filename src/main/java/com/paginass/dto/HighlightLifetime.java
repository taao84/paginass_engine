package com.paginass.dto;

import java.time.Duration;

public enum HighlightLifetime {
    ONE_DAY(Duration.ofDays(1)),
    ONE_WEEK(Duration.ofDays(7)),
    ONE_MONTH(Duration.ofDays(30));

    private final Duration duration;

    HighlightLifetime(Duration duration) {
        this.duration = duration;
    }

    public Duration getDuration() {
        return duration;
    }
}
