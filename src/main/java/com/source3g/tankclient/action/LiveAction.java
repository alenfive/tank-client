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
    private AttackService attackService;
    @Autowired
    private MapService mapService;

    @Override
    public NodeType process(GlobalValues params, List<Action> actions) {

        long seconds = (params.getSessionData().getGameOverTime().getTime()-System.currentTimeMillis())/1000;

        //剩余生命差值排序
        List<Tank> loseMingSort = params.getCurrTeam().getTanks().stream().sorted((a,b)->{
            int diff = (a.getShengming()-a.getShengyushengming())-(b.getShengming()-b.getShengyushengming());
            return diff>0?-1:1;
        }).collect(Collectors.toList());

        //队友枚举
        MapEnum[] currEnums = params.getCurrTeamTId().stream().map(MapEnum::valueOf).toArray(MapEnum[]::new);


        //如果死亡的坦克附近有敌人，并且战斗力完胜则复活
        params.getCurrTeam().getTanks().stream().filter(item->item.getShengyushengming() == 0).forEach(tank->{
            Action action = actions.stream().filter(item->item.getTId().equals(tank.getTId())).findFirst().orElse(null);

            //附近有队友
            Position itemPos = params.getSessionData().getTankLastPosList().stream().filter(item->item.getTId().equals(action.getTId())).findFirst().get().getPos();
            List<Position> teammate = mapService.findByMapEnum(params.getView(),itemPos.getRowIndex()-1,itemPos.getRowIndex()+1,itemPos.getColIndex()-1,itemPos.getColIndex()+1,currEnums);

            if(!teammate.isEmpty()){
                useGlod(params,action);
                return;
            }

            //射程范围内可攻击的坐标
            List<DiffPosition> beAttacked = attackService.beAttacked(params, itemPos,params.getEnemyTeam().getTanks());
            if(beAttacked.size() == 1){
                Position ableAttack = beAttacked.get(0).getPos();
                String ableAttackTankId = params.getView().getMap().get(ableAttack.getRowIndex()).get(ableAttack.getColIndex());
                Tank ableAttackTank = params.getEnemyTeam().getTanks().stream().filter(item->item.getTId().equals(ableAttackTankId)).findFirst().orElse(null);

                TankPosition tankPosition = attackService.ableAttackTop(params,tank,itemPos);
                //能攻击到对方
                if (tankPosition.getTank().getTId().equals(ableAttackTankId)){

                    //对敌方的剩余生命和我方复活后的生命战斗力做比较
                    int diffCurr = (ableAttackTank.getShengyushengming()+tank.getGongji()-1)/tank.getGongji();
                    int diffEnemy = (tank.getShengming()+ableAttackTank.getGongji()-1)/ableAttackTank.getGongji();
                    if(diffCurr <= diffEnemy){
                        useGlod(params,action);
                        return;
                    }
                }
            }else if(beAttacked.size() == 0){   //周末没有攻击者，复活
                if(seconds > 30 && seconds <=120){
                    useGlod(params,action);
                    return;
                }
            }
        });


        //只复活生命差值最大的坦克
        /*if(seconds>30 && seconds <=120){
            Action action = actions.stream().filter(item2->item2.getTId().equals(loseMingSort.get(0).getTId())).findFirst().orElse(null);
            useGlod(params,action);
        }*/

        //最后十秒使用所有复活币-按生命差值的顺序
        if(seconds <= 30){
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
