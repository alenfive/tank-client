package com.source3g.tankclient.action.Tank4Action;

import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.service.AttackService;
import com.source3g.tankclient.service.GlodService;
import com.source3g.tankclient.service.MapService;
import com.source3g.tankclient.service.MoveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("Duplicates")
@Service
public class TankFourService {

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

        Position targetPos = null;
        //扫图
        if(targetPos == null){
            //targetPos = buildStartPos(params,currTank,currPos);
        }
        if(targetPos == null){
            targetPos = moveService.scanMapNextPosition(params,currTank,1);
        }

        //扫完图
        if(targetPos == null){
            targetPos = moveService.buildCurrTeamPos(params,currTank);
        }

        moveService.buildMove(params,action,currTank,currPos,targetPos,true);
    }




    private Position buildStartPos(GlobalValues params, Tank tank, Position currPos) {
        TMap view = params.getView();
        List<Position> startPosList = Arrays.asList(
                new Position(0,0),
                new Position(0,view.getColLen()/2),
                new Position(0,view.getColLen()-1),
                new Position(view.getRowLen()/2,0),
                new Position(view.getRowLen()/2,view.getColLen()-1),
                new Position(view.getRowLen()-1,0),
                new Position(view.getRowLen()-1,view.getColLen()/2),
                new Position(view.getRowLen()-1,view.getColLen()-1)
        );

        TankPosition t2Pos = params.getSessionData().getTankInitPosList().stream().filter(item->item.getSuffix()==2).findFirst().orElse(null);

        List<DiffPosition> sortedStartPosList = startPosList.stream().map(item->{
            int diff = Math.abs(t2Pos.getPos().getRowIndex()-item.getRowIndex()) + Math.abs(t2Pos.getPos().getColIndex()-item.getColIndex());
            return DiffPosition.builder().diff(diff).pos(item).build();
        }).sorted(Comparator.comparing(DiffPosition::getDiff)).collect(Collectors.toList());

         Position startPos = sortedStartPosList.get(2).getPos();
         String mId = view.get(startPos.getRowIndex(),startPos.getColIndex());

         return mId.equals(MapEnum.M3.name())?startPos:null;
    }
}
