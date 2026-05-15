package com.example.export_service.service;

import com.example.export_service.dto.ExportRequest;
import com.example.export_service.model.ExportJob;

import java.util.List;

public interface ExportService {

    ExportJob exportPdf(ExportRequest request);

    ExportJob exportDocx(ExportRequest request);

    ExportJob exportJson(ExportRequest request);

    ExportJob getJobStatus(Long jobId);

    List<ExportJob> getExportsByUser(Long userId);
}
