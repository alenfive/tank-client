package com.source3g.tankclient.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientParam {
    private String team;
    private TMap view;
    private TeamDetail tA;
    private TeamDetail tB;
    private TeamDetail tC;

}
