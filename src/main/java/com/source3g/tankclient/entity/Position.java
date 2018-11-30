package com.source3g.tankclient.entity;

import lombok.Data;

/**
 * Created by alenfive1 on 17-9-17.
 */
@Data
public class Position {
    public int rowIndex = 0;
    public int colIndex = 0;

    public int F;
    public int G;
    public int H;

    public Position(int rowIndex,int colIndex){
        this.rowIndex = rowIndex;
        this.colIndex = colIndex;
    }

    public void calcF() {
        this.F = this.G + this.H;
    }

    public Position parent;
}
