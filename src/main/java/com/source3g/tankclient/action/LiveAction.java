package com.source3g.tankclient.action;

import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.service.AttackService;
import com.source3g.tankclient.service.MapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 复活币使用策略
 */
@Component
public class LiveAction extends AbstractActiion<GlobalValues,List<Action>> {

    @Autowired
    private MapService mapService;
    @Autowired
    private AttackService attackService;

    @Override
    public NodeType process(GlobalValues params, List<Action> actions) {

        TMap view = params.getView();
        long seconds = (System.currentTimeMillis()-params.getSessionData().getGameOverTime().getTime())/1000;

        //剩余生命差值排序
        List<Tank> loseMingSort = params.getCurrTeam().getTanks().stream().sorted((a,b)->{
            int diff = (a.getShengming()-a.getShengyushengming())-(b.getShengming()-b.getShengyushengming());
            return diff>0?-1:1;
        }).collect(Collectors.toList());

        //敌方坦克
        List<MapEnum> enemyEnum = params.getEnemyTeam().getTanks().stream().map(item->MapEnum.valueOf(item.getTId())).collect(Collectors.toList());

        //如果死亡的坦克附近有敌人，并且战斗力完胜则复活
        params.getCurrTeam().getTanks().stream().filter(item->item.getShengyushengming() == 0).forEach(tank->{
            Action action = actions.stream().filter(item->item.getTId().equals(tank.getTId())).findFirst().orElse(null);
            TankPosition currTankPos = params.getSessionData().getTankPositions().stream().filter(item2->tank.getTId().equals(item2.getTId())).findFirst().orElse(null);
            Position currPos = currTankPos.getPosition();
            int startR = currPos.getRowIndex()-tank.getShecheng();
            int endR = currPos.getRowIndex()+tank.getShecheng();
            int startC = currPos.getColIndex()-tank.getShecheng();
            int endC = currPos.getColIndex()+tank.getShecheng();

            //射程范围内可攻击的坐标
            List<Position> ableAttackPos = mapService.findByMapEnum(params.getView(),startR,endR,startC,endC,enemyEnum.toArray(new MapEnum[enemyEnum.size()]));
            Position ableAttack = attackService.ableAttack(view, tank,currTankPos.getPosition(),ableAttackPos,params.getEnemyTeam());
            if(ableAttack != null){
                String ableAttackTankId = params.getView().getMap().get(ableAttack.getRowIndex()).get(ableAttack.getColIndex());
                Tank ableAttackTank = params.getEnemyTeam().getTanks().stream().filter(item->item.getTId().equals(ableAttackTankId)).findFirst().orElse(null);
                int diffCurr = ableAttackTank.getShengyushengming()/tank.getGongji();
                int diffEnemy = tank.getShengming()/ableAttackTank.getGongji();

                if((diffCurr-diffEnemy)>1){
                    useGlod(params,action);
                }
            }
        });

        //只复活生命差值最大的坦克
        if(seconds>0 && seconds <=60){
            Action action = actions.stream().filter(item2->item2.getTId().equals(loseMingSort.get(0).getTId())).findFirst().orElse(null);
            useGlod(params,action);
        }

        //最后十秒使用所有复活币-按生命差值的顺序
        if(seconds <= 10){
            loseMingSort.forEach(item1->{
                Action action =  actions.stream().filter(item2->item2.getTId().equals(item1.getTId())).findFirst().orElse(null);
                useGlod(params,action);
            });
        }

        return NodeType.Success;
    }

    private void useGlod(GlobalValues params, Action action){
        params.getCurrTeam().setGlod(params.getCurrTeam().getGlod()-1);
        //死亡后使用复活币
        action.setUseGlod(true);
    }
}
