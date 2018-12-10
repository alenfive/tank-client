package com.source3g.tankclient.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Created by alenfive1 on 17-9-17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return rowIndex == position.rowIndex &&
                colIndex == position.colIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowIndex, colIndex);
    }
}
