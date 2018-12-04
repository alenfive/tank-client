package com.source3g.tankclient.service;

import com.source3g.tankclient.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 攻击
 */
@Service
public class AttackService {

    @Autowired
    private MapService mapService;

    //默认攻击顺序
    private final List<String> tIdSorted = Arrays.asList("C1","B1","C3","B3","C4","B4","C5","B5","C2","B2","A1");

    /**
     * 返回可攻击敌方坐标
     *
     * @param view
     * @param currTank
     * @param currPos
     * @param targetPosList
     * @return
     */
    public Position ableAttack(TMap view, Tank currTank, Position currPos, List<Position> targetPosList,TeamDetail enemyTanks) {

        List<TankPosition> ablePos = new ArrayList<>();
        for(Position targetPos : targetPosList){
            int diff = Math.abs(targetPos.getRowIndex()-currPos.getRowIndex())+Math.abs(targetPos.getColIndex()-currPos.getColIndex());
            boolean ableAttack = (currPos.getRowIndex()==targetPos.getRowIndex() || currPos.getColIndex() == targetPos.getColIndex()) && diff<=currTank.getShecheng();
            if(ableAttack){
                String tId = view.getMap().get(targetPos.getRowIndex()).get(targetPos.getColIndex());
                ablePos.add(TankPosition.builder().tId(tId).position(targetPos).build());
            }
        }
        if(ablePos.isEmpty())return null;

        //返回优先攻击对象
        //第一优先-一枪打死
        List<TankPosition> oneAttacks = new ArrayList<>();
        for(TankPosition tp : ablePos){
            for(Tank enemyTank : enemyTanks.getTanks()){
                if(tp.getTId().equals(enemyTank.getTId()) && enemyTank.getShengyushengming()<=currTank.getGongji()){
                    oneAttacks.add(tp);
                }
            }
        }

        if(!oneAttacks.isEmpty()){
            for (String tId : tIdSorted){
                for (TankPosition tp : oneAttacks){
                    if (tId.equals(tp.getTId())){
                        return tp.getPosition();
                    }
                }
            }
        }else{
            //第二优先-攻击顺序
            for (String tId : tIdSorted){
                for (TankPosition tp : ablePos){
                    if (tId.equals(tp.getTId())){
                        return tp.getPosition();
                    }
                }
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
        action.setUsed(true);

        //打死BOSS后地图上移除
        if(targetTank.getShengyushengming()-currTank.getGongji() <=0){
            view.getMap().get(targetPos.getRowIndex()).set(targetPos.getColIndex(),MapEnum.M1.name());
        }

    }

}
