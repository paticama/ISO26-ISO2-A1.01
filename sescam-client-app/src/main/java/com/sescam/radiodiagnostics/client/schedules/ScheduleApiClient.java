package com.sescam.radiodiagnostics.client.schedules;

import com.sescam.radiodiagnostics.common.schedules.OpenScheduleRequestDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleChangeResultDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleSearchCriteriaDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleStatusChangeRequestDTO;
import com.sescam.radiodiagnostics.common.schedules.ScheduleSummaryDTO;
import com.sescam.radiodiagnostics.common.schedules.UpdateScheduleRequestDTO;

import java.util.List;

/**
 * Client-side port to the schedules REST API of UC-AAD-02. Every method throws
 * {@link ScheduleClientException} when the server rejects the request or cannot be reached.
 */
public interface ScheduleApiClient {

    List<ScheduleSummaryDTO> search(ScheduleSearchCriteriaDTO criteria, String actorId, String actorRole);

    ScheduleDTO get(String scheduleId, String actorId, String actorRole);

    ScheduleChangeResultDTO open(OpenScheduleRequestDTO request);

    ScheduleChangeResultDTO update(UpdateScheduleRequestDTO request);

    ScheduleChangeResultDTO close(ScheduleStatusChangeRequestDTO request);

    ScheduleChangeResultDTO reopen(ScheduleStatusChangeRequestDTO request);
}
