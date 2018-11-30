package com.source3g.tankclient.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Action {
    private String tId;
    private DirectionEnum direction;
    private ActionTypeEnum type;
    private Integer length;
    private boolean useGlod;

    @JsonProperty("tId")
    public String getTId() {
        return tId;
    }
}
