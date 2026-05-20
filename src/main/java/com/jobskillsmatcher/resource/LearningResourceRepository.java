package com.jobskillsmatcher.resource;

import com.jobskillsmatcher.resource.impl.jpa.LearningResource;
import com.jobskillsmatcher.resource.model.ResourceType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LearningResourceRepository extends JpaRepository<LearningResource, UUID> {

    @Query("""
            select r from LearningResource r
            where (:query is null or :query = '' or lower(r.title) like lower(concat('%', :query, '%')))
              and (:type is null or r.type = :type)
            order by r.title asc
            """)
    Page<LearningResource> search(@Param("query") String query,
                                  @Param("type") ResourceType type,
                                  Pageable pageable);

    @Query("""
            select r from LearningResource r
            where r.urlAlive = true
              and r.id in (
                select rs.id.resourceId from ResourceSkill rs
                where rs.id.skillId in :skillIds
              )
            """)
    List<LearningResource> findAllForSkillIds(@Param("skillIds") Collection<String> skillIds);
}
