package com.source3g.tankclient.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by alenfive1 on 17-9-17.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TankPosition {
    private String tId;
    private Position position;
}
