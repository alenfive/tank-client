package com.source3g.tankclient.action.Tank5Action;

import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.service.AttackService;
import com.source3g.tankclient.service.GlodService;
import com.source3g.tankclient.service.MapService;
import com.source3g.tankclient.service.MoveService;
import com.source3g.tankclient.utils.AStar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("Duplicates")
@Service
public class TankFiveService {

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


        //逃离
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

        //扫完图
        Position targetPos = buildCenterPos(params,currPos);
        moveService.buildMove(params,action,currTank,currPos,targetPos,true);
    }

    private Position buildCenterPos(GlobalValues params,Position currPos) {
        Position targetPos = new Position(params.getView().getRowLen()/2,params.getView().getColLen()/2);
        mapService.buildBlank(params,currPos,targetPos);
        return targetPos;
    }





}
