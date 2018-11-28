package com.source3g.tankclient.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InitParam {
    private String map;
    @JsonProperty(value = "tB")
    private Team tB;
    @JsonProperty(value = "tC")
    private Team tC;
}
