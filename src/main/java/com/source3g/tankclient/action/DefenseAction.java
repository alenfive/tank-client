package com.source3g.tankclient.action;

import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.service.MapService;
import com.source3g.tankclient.service.MoveService;
import com.source3g.tankclient.utils.AStar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 防御
 * 条件：
 * 1、复活币捡完
 * 2、BOSS打完
 * 3、已经胜利
 */
@Component
public class DefenseAction extends AbstractActiion<GlobalValues,Action> {

    @Autowired
    private MapService mapService;
    @Autowired
    private MoveService moveService;

    @Override
    public NodeType process(GlobalValues params, Action action) {

        //是否可防御
        if(!ableDefense(params)){
            return NodeType.Failure;
        }

        TMap view = params.getView();
        Position currPos = mapService.getPosition(params.getView(),action.getTId());
        Tank tank = params.currTeam.getTanks().stream().filter(item->item.getTId().equals(action.getTId())).findFirst().orElse(null);
        AStar aStar = new AStar(view);
        Position nextPos = null;
        //最近的队友
        Position minLenPos = findGatherPosition(params,currPos,tank);

        int scope = 2;  //在半径2内才算集合成功
        //队友未死光,开始集合
        if(minLenPos != null){
            int diff = Math.abs(currPos.getRowIndex()-minLenPos.getRowIndex()) + Math.abs(currPos.getColIndex()-minLenPos.getColIndex());
            Position blankPos = findRoundBlank(view,minLenPos,scope);
            if(diff > scope && blankPos != null){ //未完成集合
                aStar.clear();
                nextPos = aStar.findPath(currPos,blankPos);

                return executeMove(params,view,tank,action,currPos,nextPos);
            }
        }

        //集和完毕，开始定位安全屋
        Position targetPos = findDefensePosition(view,tank,scope);
        aStar.clear();
        nextPos = aStar.findPath(currPos,targetPos);
        return executeMove(params,view,tank,action,currPos,nextPos);
    }

    /**
     * 定位安全屋-最近的角落
     * @param view
     * @param tank
     * @param scope
     * @return
     */
    private Position findDefensePosition(TMap view, Tank tank, int scope) {
        Position pos = new Position(0,view.getColLen()-1);
        return findRoundBlank(view,pos,scope);
    }

    private NodeType executeMove(GlobalValues params,TMap view, Tank tank,Action action, Position currPos, Position nextPos) {
        nextPos = mapService.getMaxNext(tank,currPos,nextPos);

        //允许下一步，替换地图
        if(nextPos != null){
            view.getMap().get(currPos.getRowIndex()).set(currPos.getColIndex(),MapEnum.M1.name());
            view.getMap().get(nextPos.getRowIndex()).set(nextPos.getColIndex(),action.getTId());

            //根据坐标，计算方位和步长
            moveService.buildAction(params,action,currPos,nextPos);
        }

        return NodeType.Success;
    }


    /**
     * 锁定队友附近的落脚点
     * @param targetPos
     * @param scope
     * @return
     */
    private Position findRoundBlank(TMap view,Position targetPos, int scope) {
        int startR = targetPos.getRowIndex()-scope;
        int endR = targetPos.getRowIndex()+scope;
        int startC = targetPos.getColIndex()-scope;
        int endC = targetPos.getColIndex()+scope;

        List<Position> poss = mapService.findByMapEnum(view,startR,endR,startC,endC,MapEnum.M1);
        if(poss.isEmpty()){
            return null;
        }
        return poss.get(0);
    }

    private Position findGatherPosition(GlobalValues params, Position currPos, Tank tank) {
        Position gatherPos = null;
        int gatherDiff = 0;
        for(Tank item : params.getCurrTeam().getTanks()){
            if (item.getTId().equals(tank.getTId())){
                continue;
            }
            Position itemPos = mapService.getPosition(params.getView(),item.getTId());
            if(itemPos == null){
                continue;
            }

            int diff = Math.abs(currPos.getRowIndex()-itemPos.getRowIndex()) + Math.abs(currPos.getColIndex()-itemPos.getColIndex());

            if (gatherPos == null){
                gatherPos = itemPos;
                gatherDiff = diff;
                continue;
            }

            if(diff > gatherDiff){
                continue;
            }

            gatherPos = itemPos;
            gatherDiff = diff;
        }
        return gatherPos;
    }


    /**
     * 防御条件
     * @return
     */
    private boolean ableDefense(GlobalValues params) {
        //复活币或BOSS已被捡光和杀死 != 3(2个复活币，1个BOSS)
        int totalExtend = params.getCurrTeam().getExtend() + params.getEnemyTeam().getExtend();
        if(totalExtend >= 3){
            return true;
        }

        //生命值倒序排列
        List<Tank> enemyShengMingList =  params.getEnemyTeam().getTanks().stream().sorted((a, b)->{
            int aDiff = a.getShengming()-a.getShengyushengming();
            int bDiff = b.getShengming()-b.getShengyushengming();
            return aDiff>=bDiff?-1:1;
        }).collect(Collectors.toList());

        //生命值倒序排列
        List<Tank> currShengMingList =  params.getEnemyTeam().getTanks().stream().sorted((a, b)->{
            int aDiff = a.getShengming()-a.getShengyushengming();
            int bDiff = b.getShengming()-b.getShengyushengming();
            return aDiff>=bDiff?-1:1;
        }).collect(Collectors.toList());

        Integer enemyAddShengMing = 0;
        Integer currAddShengMing = 0;
        //计算有复活币时能加多少生命
        for(int i=0;i<params.getEnemyTeam().getGlod();i++){
            enemyAddShengMing +=enemyShengMingList.get(i).getShengming()-enemyShengMingList.get(i).getShengyushengming();
        }

        for(int i=0;i<params.getCurrTeam().getGlod();i++){
            currAddShengMing += currShengMingList.get(i).getShengming()-currShengMingList.get(i).getShengyushengming();
        }

        Integer enemyTeamShengmin = params.getEnemyTeam().getTanks().stream().map(item->item.getShengyushengming()).reduce(Integer::sum).get();
        Integer currTeamShengmin = params.getCurrTeam().getTanks().stream().map(item->item.getShengyushengming()).reduce(Integer::sum).get();

        enemyTeamShengmin += enemyAddShengMing;
        currTeamShengmin += currAddShengMing;

        //如果敌方血量小于自身
        if(enemyTeamShengmin < currTeamShengmin){
            return true;
        }

        return false;
    }
}
