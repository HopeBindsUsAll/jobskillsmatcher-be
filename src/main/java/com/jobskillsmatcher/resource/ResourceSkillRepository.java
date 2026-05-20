package com.jobskillsmatcher.resource;

import com.jobskillsmatcher.resource.impl.jpa.ResourceSkillId;

import com.jobskillsmatcher.resource.impl.jpa.ResourceSkill;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ResourceSkillRepository extends JpaRepository<ResourceSkill, ResourceSkillId> {

    List<ResourceSkill> findAllByIdResourceId(UUID resourceId);

    List<ResourceSkill> findAllByIdResourceIdIn(Collection<UUID> resourceIds);

    List<ResourceSkill> findAllByIdSkillIdIn(Collection<String> skillIds);

    @Modifying
    @Query("delete from ResourceSkill rs where rs.id.resourceId = :resourceId")
    int deleteByResourceId(@Param("resourceId") UUID resourceId);
}
