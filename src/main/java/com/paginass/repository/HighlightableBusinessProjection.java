package com.paginass.repository;

import java.util.UUID;

public interface HighlightableBusinessProjection {
    UUID getId();
    String getName();
    String getBizType();
}
