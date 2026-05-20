package com.jobskillsmatcher.cv;

import com.jobskillsmatcher.cv.impl.jpa.CvUpload;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CvUploadRepository extends JpaRepository<CvUpload, UUID> {

    Page<CvUpload> findByStudentIdOrderByUploadedAtDesc(UUID studentId, Pageable pageable);
}
