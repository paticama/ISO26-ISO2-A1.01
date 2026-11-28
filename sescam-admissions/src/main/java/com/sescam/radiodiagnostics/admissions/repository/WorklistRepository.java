package com.sescam.radiodiagnostics.admissions.repository;

import com.sescam.radiodiagnostics.admissions.domain.WorklistItem;

import java.time.LocalDate;
import java.util.List;

public interface WorklistRepository {

    WorklistItem save(WorklistItem item);

    List<WorklistItem> findByDate(LocalDate date);
}
