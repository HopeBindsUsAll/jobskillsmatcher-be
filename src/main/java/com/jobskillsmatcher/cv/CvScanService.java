package com.jobskillsmatcher.cv;

import com.jobskillsmatcher.cv.port.rest.CvScanResultView;
import com.jobskillsmatcher.cv.port.rest.CvUploadSummaryView;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface CvScanService {

    CvScanResultView scan(UUID studentId, MultipartFile file);

    Page<CvUploadSummaryView> listForStudent(UUID studentId, int page, int size);

    CvScanResultView getForStudent(UUID studentId, UUID uploadId);
}
