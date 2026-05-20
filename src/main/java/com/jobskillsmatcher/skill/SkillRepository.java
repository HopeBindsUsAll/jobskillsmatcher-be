package com.jobskillsmatcher.skill;

import com.jobskillsmatcher.skill.impl.jpa.Skill;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, String> {

    @Query(value = """
            select s.*
            from skill s
            where similarity(s.preferred_label, :query) > 0.1
               or lower(s.preferred_label) like lower(concat('%', :query, '%'))
            order by similarity(s.preferred_label, :query) desc
            limit :limit
            """, nativeQuery = true)
    List<Skill> searchByTrigram(@Param("query") String query, @Param("limit") int limit);
}
