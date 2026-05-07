package com.paginass.repository;

public interface BusinessLocationProjection {
    String getId();
    String getName();
    String getSlug();
    String getBizType();
    Double getLatitude();
    Double getLongitude();
}
