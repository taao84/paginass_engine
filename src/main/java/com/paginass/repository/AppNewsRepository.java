package com.paginass.repository;

import com.paginass.entity.AppNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AppNewsRepository extends JpaRepository<AppNews, UUID> {

    @Query("SELECT n FROM AppNews n ORDER BY n.createdAt DESC LIMIT :limit OFFSET :offset")
    List<AppNews> findAllPaged(@Param("limit") int limit, @Param("offset") int offset);
}
