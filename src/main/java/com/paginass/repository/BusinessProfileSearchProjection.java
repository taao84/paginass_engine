package com.paginass.repository;

public interface BusinessProfileSearchProjection {
    String getId();
    String getName();
    String getSlug();
    String getBizType();
    String getDescription();
    String getTags();
    String getContactInfo();
    Double getScore();
    Boolean getIsClaimed();
    Boolean getDeliversToHome();
}
