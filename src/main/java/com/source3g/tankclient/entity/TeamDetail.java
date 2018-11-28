package com.source3g.tankclient.entity;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TeamDetail {
    private Integer glod;
    private String name;
    private String serverUrl;
    private List<Tank> tanks;
}
