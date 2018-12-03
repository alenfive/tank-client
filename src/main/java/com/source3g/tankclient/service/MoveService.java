package com.source3g.tankclient.service;

import com.source3g.tankclient.entity.Action;
import com.source3g.tankclient.entity.ActionTypeEnum;
import com.source3g.tankclient.entity.DirectionEnum;
import com.source3g.tankclient.entity.Position;
import org.springframework.stereotype.Service;

/**
 * 移动
 */
@Service
public class MoveService {

    public void buildAction(Action action, Position currPos, Position nextPos) {
        int rowDiff = nextPos.getRowIndex()-currPos.getRowIndex();
        int colDiff = nextPos.getColIndex()-currPos.getColIndex();
        action.setDirection(rowDiff>0?DirectionEnum.DOWN:rowDiff<0?DirectionEnum.UP:colDiff>0?DirectionEnum.RIGHT:colDiff<0?DirectionEnum.LEFT:DirectionEnum.WAIT);
        action.setLength(Math.abs(rowDiff!=0?rowDiff:colDiff));
        action.setType(ActionTypeEnum.MOVE);
        action.setUsed(true);
    }

}
