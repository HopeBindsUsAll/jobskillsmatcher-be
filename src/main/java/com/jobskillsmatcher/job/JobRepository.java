package com.jobskillsmatcher.job;

import com.jobskillsmatcher.job.impl.jpa.Job;

import com.jobskillsmatcher.job.model.Seniority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByExternalId(String externalId);

    @Query("""
            select j from Job j
            where (:country is null or :country = '' or j.country = :country)
              and (:city is null or :city = '' or lower(j.city) = lower(:city))
              and (:remoteOnly = false or j.remote = true)
            order by j.postedAt desc nulls last, j.createdAt desc
            """)
    Page<Job> filter(@Param("country") String country,
                           @Param("city") String city,
                           @Param("remoteOnly") boolean remoteOnly,
                           Pageable pageable);

    // Recent jobs matching the filters, fed into the student feed for scoring.
    @Query("""
            select j from Job j
            where (:country is null or :country = '' or j.country = :country)
              and (:city is null or :city = '' or lower(j.city) = lower(:city))
              and (:remoteOnly = false or j.remote = true)
              and (:seniority is null or j.seniority = :seniority)
              and (:search is null or :search = ''
                   or lower(j.title) like lower(concat('%', :search, '%'))
                   or lower(j.company) like lower(concat('%', :search, '%')))
            order by j.postedAt desc nulls last, j.createdAt desc
            """)
    List<Job> rankingPool(@Param("country") String country,
                                @Param("city") String city,
                                @Param("remoteOnly") boolean remoteOnly,
                                @Param("seniority") Seniority seniority,
                                @Param("search") String search,
                                Pageable pageable);

    // Jobs whose title contains the role keyword. Used by CV scan to suggest missing skills.
    @Query("""
            select j from Job j
            where (:country is null or :country = '' or j.country = :country)
              and lower(j.title) like lower(concat('%', :role, '%'))
            order by j.postedAt desc nulls last, j.createdAt desc
            """)
    List<Job> findByPreferredRole(@Param("role") String role,
                                        @Param("country") String country,
                                        Pageable pageable);
}
