package com.source3g.tankclient.action.Tank1Action;

import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.service.AttackService;
import com.source3g.tankclient.service.MapService;
import com.source3g.tankclient.service.MoveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("Duplicates")
@Service
public class TankOneService {

    @Autowired
    private MoveService moveService;
    @Autowired
    private MapService mapService;
    @Autowired
    private AttackService attackService;


    public void action(GlobalValues params, Action action){

        if(action.isUsed())return;

        TMap view = params.getView();
        Position currPos = mapService.getPosition(params.getView(),action.getTId());
        Tank currTank = params.currTeam.getTanks().stream().filter(item->item.getTId().equals(action.getTId())).findFirst().orElse(null);


        //逃跑
        Position leavePos = moveService.buildLeave(params,currTank,currPos);
        if(leavePos != null){
            moveService.buildMove(params,action,currTank,currPos,leavePos,false);
            return;
        }

        //攻击敌方
        TankPosition ableAttackPos = attackService.ableAttackTop(params,currTank,currPos);
        if (ableAttackPos != null){
            attackService.attackTank(view,currTank,action,currPos,ableAttackPos.getPos(),ableAttackPos.getTank());
            return;
        }

        //是否有移动后能攻击到并打赢的位置
        Position ableMoveAttackPos = ableMoveAttackPos(params,currTank,currPos);
        if (ableMoveAttackPos != null){
            moveService.buildMove(params,action,currTank,currPos,ableMoveAttackPos,false);
            return;
        }

        //攻击boss
        TankPosition ableAttackBoss = attackService.buildAbleAttackBossPos(params,currTank,currPos);
        if (ableAttackBoss != null){
            attackService.attackTank(view,currTank,action,currPos,ableAttackBoss.getPos(),ableAttackBoss.getTank());
            return;
        }

        //存在攻击目标-向站位靠近
        if (params.getAttackTarget() != null){
            Position preTargetPos = attackService.buildPrepareAttackPos(params,params.getAttackTarget(),currTank,currPos);
            if (preTargetPos != null){
                moveService.buildMove(params,action,currTank,currPos,preTargetPos,true);
                return;
            }
        }

        //巡回
        Position target = buildCenterPos(params,currPos);
        moveService.buildMove(params,action,currTank,currPos,target,true);
    }

    //是否有移动后能攻击到并打赢的位置
    private Position ableMoveAttackPos(GlobalValues params, Tank currTank, Position currPos) {
        List<Position> ableMovePos = Arrays.asList(
                new Position(currPos.getRowIndex()-1,currPos.getColIndex()),
                new Position(currPos.getRowIndex(),currPos.getColIndex()+1),
                new Position(currPos.getRowIndex()+1,currPos.getColIndex()),
                new Position(currPos.getRowIndex(),currPos.getColIndex()-1)
        );

        ableMovePos = ableMovePos.stream().filter(item->{

            if(!mapService.isPosition(params.getView(),item)){
                return false;
            }
            String mId = params.getView().get(item.getRowIndex(),item.getColIndex());
            if(mapService.isBlockTrue(mId)){
                return false;
            }

            //移动后的这个点周围是否有敌人
            List<Position> posList = Arrays.asList(
                    new Position(item.getRowIndex()-1,item.getColIndex()),
                    new Position(item.getRowIndex(),item.getColIndex()+1),
                    new Position(item.getRowIndex()+1,item.getColIndex()),
                    new Position(item.getRowIndex(),item.getColIndex()-1)
            );

            posList = posList.stream().filter(item2->{
                if(!mapService.isPosition(params.getView(),item2)){
                    return false;
                }
                String mItem2Id = params.getView().get(item2.getRowIndex(),item2.getColIndex());
                return params.getEnemyTeamTId().contains(mItem2Id);
            }).collect(Collectors.toList());

            //这个点周围没有敌方
            if (posList.isEmpty()){
                return false;
            }

            //这个点会被0个或多个敌方锁定
            List<DiffPosition> beAttackeds = attackService.beAttacked(params,item,params.getEnemyTeam().getTanks()).stream().collect(Collectors.toList());
            if (beAttackeds.size() != 1){
                return false;
            }

            //攻击力承受不住多一次的伤害
            int diffCurr = (beAttackeds.get(0).getTank().getShengyushengming()+currTank.getGongji()-1)/currTank.getGongji();
            int diffEnemy = (currTank.getShengyushengming()+beAttackeds.get(0).getTank().getGongji()-1)/beAttackeds.get(0).getTank().getGongji();
            if( diffCurr >= diffEnemy){
                return false;
            }
            return true;
        }).collect(Collectors.toList());

        return ableMovePos.isEmpty()?null:ableMovePos.get(0);
    }

    private Position buildCenterPos(GlobalValues params,Position currPos) {
        Position targetPos = new Position(params.getView().getRowLen()/2,params.getView().getColLen()/2);
        mapService.buildBlank(params,currPos,targetPos);
        return targetPos;
    }
}
