package com.jobskillsmatcher.user;

import com.jobskillsmatcher.user.impl.jpa.User;

import com.jobskillsmatcher.user.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByGoogleSub(String googleSub);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            select u from User u
            where (:role is null or u.role = :role)
              and (:enabled is null or u.enabled = :enabled)
              and (:query = '' or lower(u.email) like lower(concat('%', :query, '%')))
            order by u.createdAt desc
            """)
    Page<User> filter(@Param("role") Role role,
                            @Param("enabled") Boolean enabled,
                            @Param("query") String query,
                            Pageable pageable);
}
