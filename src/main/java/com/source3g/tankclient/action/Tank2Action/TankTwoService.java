package com.source3g.tankclient.action.Tank2Action;

import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.service.AttackService;
import com.source3g.tankclient.service.GlodService;
import com.source3g.tankclient.service.MapService;
import com.source3g.tankclient.service.MoveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@SuppressWarnings("Duplicates")
@Service
public class TankTwoService {
    @Autowired
    private MoveService moveService;
    @Autowired
    private MapService mapService;
    @Autowired
    private GlodService glodService;
    @Autowired
    private AttackService attackService;

    public void action(GlobalValues params, Action action){
        if(action.isUsed())return;

        TMap view = params.getView();
        Position currPos = mapService.getPosition(params.getView(),action.getTId());
        Tank currTank = params.currTeam.getTanks().stream().filter(item->item.getTId().equals(action.getTId())).findFirst().orElse(null);

        //有复活币
        Position glodPos = glodService.buildGlodPos(params,currTank,currPos);
        if (glodPos != null){
            glodService.buildGlodMove(params,action,currTank,currPos,glodPos);
            return;
        }

        //逃离
        Position leavePos = moveService.buildLeave(params,currTank,currPos);
        if(leavePos != null){
            moveService.buildMove(params,action,currTank,currPos,leavePos,false);
            return;
        }

        //攻击敌方
        TankPosition ableAttackPos = attackService.ableAttackTop(params,currTank,currPos);
        if (ableAttackPos != null){
            attackService.attackTank(params,view,currTank,action,currPos,ableAttackPos.getPos(),ableAttackPos.getTank());
            return;
        }

        //攻击boss
        TankPosition ableAttackBoss = attackService.buildAbleAttackBossPos(params,currTank,currPos);
        if (ableAttackBoss != null){
            attackService.attackTank(params,view,currTank,action,currPos,ableAttackBoss.getPos(),ableAttackBoss.getTank());
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

    private Position buildCenterPos(GlobalValues params,Position currPos) {
        Position targetPos = new Position(params.getView().getRowLen()/2,params.getView().getColLen()/2);
        mapService.buildBlank(params,currPos,targetPos);
        return targetPos;
    }
}

