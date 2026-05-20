package com.jobskillsmatcher.auditlog.impl;

import com.jobskillsmatcher.auditlog.impl.jpa.AuditLog;
import com.jobskillsmatcher.auditlog.model.AuditLogFilter;
import com.jobskillsmatcher.user.impl.jpa.User;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;


public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> matching(AuditLogFilter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.category() != null) {
                p.add(cb.equal(root.get("category"), f.category()));
            }
            if (f.level() != null) {
                p.add(cb.equal(root.get("level"), f.level()));
            }
            if (f.outcome() != null) {
                p.add(cb.equal(root.get("outcome"), f.outcome()));
            }
            if (f.action() != null) {
                p.add(cb.equal(root.get("action"), f.action()));
            }
            if (f.role() != null && !f.role().isBlank()) {
                p.add(cb.equal(root.get("actorRole"), f.role().trim()));
            }
            if (f.from() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.from()));
            }
            if (f.to() != null) {
                p.add(cb.lessThanOrEqualTo(root.get("createdAt"), f.to()));
            }
            if (f.email() != null && !f.email().isBlank()) {
                String like = "%" + f.email().trim().toLowerCase() + "%";
                Subquery<String> sub = query.subquery(String.class);
                Root<User> u = sub.from(User.class);
                sub.select(u.get("email"))
                        .where(cb.equal(u.get("id"), root.get("actorUserId")));
                Expression<String> resolved =
                        cb.coalesce(root.<String>get("actorEmail"), sub);
                p.add(cb.like(cb.lower(resolved), like));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}
