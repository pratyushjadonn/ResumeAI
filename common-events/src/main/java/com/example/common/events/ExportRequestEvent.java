package com.example.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequestEvent {

    private Long resumeId;
    private String userEmail;
    private String exportType;
}
