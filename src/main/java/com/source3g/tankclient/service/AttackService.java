package com.source3g.tankclient.service;

import com.source3g.tankclient.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 攻击
 */
@Service
public class AttackService {

    @Autowired
    private MapService mapService;

    /**
     * 返回可攻击敌方坐标
     * @param currTank
     * @param currPos
     * @param targetPosList
     * @return
     */
    public Position ableAttack(Tank currTank, Position currPos, List<Position> targetPosList) {

        for(Position targetPos : targetPosList){
            int diff = Math.abs(targetPos.getRowIndex()-currPos.getRowIndex())+Math.abs(targetPos.getColIndex()-currPos.getColIndex());
            boolean ableAttack = (currPos.getRowIndex()==targetPos.getRowIndex() || currPos.getColIndex() == targetPos.getColIndex()) && diff<=currTank.getShecheng();
            if(ableAttack){
                return targetPos;
            }
        }

        return null;
    }

    /**
     * 攻击
     * @param view
     * @param currTank
     * @param action
     * @param currPos
     * @param targetPos
     * @param targetTank
     */
    public void attackTank(TMap view, Tank currTank, Action action, Position currPos, Position targetPos, Tank targetTank) {
        DirectionEnum direct = DirectionEnum.UP;
        if(currPos.getRowIndex()<targetPos.getRowIndex()){
            direct = DirectionEnum.DOWN;
        }else if(currPos.getColIndex()<targetPos.getColIndex()){
            direct = DirectionEnum.RIGHT;
        }else if(currPos.getColIndex()>targetPos.getColIndex()){
            direct = DirectionEnum.LEFT;
        }
        int diff = Math.abs(targetPos.getRowIndex()-currPos.getRowIndex())+Math.abs(targetPos.getColIndex()-currPos.getColIndex());
        action.setDirection(direct);
        action.setType(ActionTypeEnum.FIRE);
        action.setLength(diff);

        //打死BOSS后地图上移除
        if(targetTank.getShengyushengming()-currTank.getGongji() <=0){
            view.getMap().get(targetPos.getRowIndex()).set(targetPos.getColIndex(),MapEnum.M1.name());
        }

    }

}
