package com.source3g.tankclient.action;

import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.service.MapService;
import com.source3g.tankclient.service.MoveService;
import com.source3g.tankclient.utils.AStar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 撤退
 * 条件：
 * 1、打不赢
 */
@Component
public class RetreatAction extends AbstractActiion<GlobalValues,Action> {

    @Autowired
    private MapService mapService;
    @Autowired
    private MoveService moveService;

    @Override
    public NodeType process(GlobalValues params, Action action) {

        AStar aStar = new AStar(params.getView());
        Position currPos = mapService.getPosition(params.getView(),action.getTId());
        Tank tank = params.currTeam.getTanks().stream().filter(item->item.getTId().equals(action.getTId())).findFirst().orElse(null);

        //最近的敌人
        DiffPosition diffEnemyPos = findMinLenEnemyPosition(params.getView(),aStar,params.getEnemyTeam(),currPos,tank.getTId());
        if(!ableRetreat(diffEnemyPos,tank)){
            return NodeType.Failure;
        }

        Position nextPos = buildAbleNextPosition(params,diffEnemyPos,currPos,tank);
        if(nextPos == null){
            return NodeType.Failure;
        }

        moveService.buildAction(action,currPos,nextPos);

        return NodeType.Success;
    }

    /**
     * 撤退可选点
     * @param params
     * @param enemyDiffPos
     * @param currPos
     * @param currTank
     * @return
     */
    private Position buildAbleNextPosition(GlobalValues params, DiffPosition enemyDiffPos, Position currPos, Tank currTank) {

        TMap view = params.getView();

        int startR = currPos.getRowIndex()-currTank.getYidong();
        int endR = currPos.getRowIndex()+currTank.getYidong();
        int startC = currPos.getColIndex()-currTank.getYidong();
        int endC = currPos.getColIndex()+currTank.getYidong();

        List<Position> ablePos = mapService.findByMapEnum(params.getView(),startR,endR,startC,endC,MapEnum.M1,MapEnum.M2,MapEnum.M3);
        //过滤斜边
        ablePos = ablePos.stream().filter(item->item.getRowIndex()==currPos.getRowIndex() || item.getColIndex() == currPos.getColIndex()).collect(Collectors.toList());
        //过滤可被攻击的点
        ablePos = ablePos.stream().filter(item->!ableAttack(item,enemyDiffPos)).collect(Collectors.toList());

        AStar aStar = new AStar(view);

        //返回离队友最近的点
        Position minPox = ablePos.stream().max((a,b)->{
            DiffPosition aDiffPos = findMinLenEnemyPosition(view,aStar,params.getCurrTeam(),a,currTank.getTId());
            DiffPosition bDiffPos = findMinLenEnemyPosition(view,aStar,params.getCurrTeam(),b,currTank.getTId());
            return aDiffPos.getDiff() - bDiffPos.getDiff();
        }).orElse(null);

        return minPox;
    }

    /**
     * 可攻击判断，在同一条直线上，并且在射程内
     * @param item
     * @param enemyDiffPos
     * @return
     */
    private boolean ableAttack(Position item, DiffPosition enemyDiffPos) {
        int absRow = Math.abs(enemyDiffPos.getPos().getRowIndex()-item.getRowIndex());
        int absCol = Math.abs(enemyDiffPos.getPos().getColIndex() - item.getColIndex());
        int diff =  absRow + absCol;
        if((absRow == 0 || absCol == 0) && diff <= enemyDiffPos.getTank().getShecheng()){
            return true;
        }
        return false;
    }

    private boolean ableRetreat(DiffPosition diffPos,Tank currTank) {
        if(diffPos == null || diffPos.getPos() == null)return false;

        //近在眼前可被一枪打死,不撤退
        if(diffPos.getDiff() == 1 && diffPos.getTank().getShengyushengming()<=currTank.getGongji()){
            return false;
        }

        //距离过远，不撤退
        if(diffPos.getDiff()>4){
            return false;
        }

        return true;
    }

    /**
     * 最近的点
     * @param view
     * @param targetTeam
     * @param currPos
     * @param currTankId
     * @return
     */
    private DiffPosition findMinLenEnemyPosition(TMap view,AStar aStar,TeamDetail targetTeam, Position currPos, String currTankId) {
        DiffPosition diffPosition = new DiffPosition();
        for(Tank item : targetTeam.getTanks()){
            if (item.getTId().equals(currTankId)){
                continue;
            }
            Position itemPos = mapService.getPosition(view,item.getTId());
            if(itemPos == null){
                continue;
            }
            aStar.resetBlockList(MapEnum.M1,MapEnum.M2,MapEnum.M3,MapEnum.valueOf(item.getTId()));
            int diff = aStar.countStep(currPos,itemPos);

            if (diffPosition.getPos() != null && diff > diffPosition.getDiff()){
                continue;
            }

            diffPosition.setPos(itemPos);
            diffPosition.setDiff(diff);
            diffPosition.setTank(item);
        }
        return diffPosition;
    }

}
