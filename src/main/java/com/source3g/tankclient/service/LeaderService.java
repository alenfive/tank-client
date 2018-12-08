package com.source3g.tankclient.service;

import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.utils.AStar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 攻击
 */
@Service
public class LeaderService {

    @Autowired
    private MapService mapService;
    @Autowired
    private MoveService moveService;

    //获得中心的某个空白点
    public Position buildCenterBlank(GlobalValues params) {

        Position target = new Position(params.getView().getRowLen()/2,params.getView().getColLen()/2);
        String centerMId = params.getView().get(target.getRowIndex(),target.getColIndex());
        Integer currShengmin = params.getCurrTeam().getTanks().stream().map(item->item.getShengyushengming()).reduce(Integer::sum).get();
        Integer enemyShengmin = params.getEnemyTeam().getTanks().stream().map(item->item.getShengyushengming()).reduce(Integer::sum).get();

        //还有资源，或者敌方相对残血时，继续扫描未知区域
        boolean isM3 = MapEnum.M3.name().equals(centerMId);
        boolean haveResource = params.getEnemyTeam().getExtend() + params.getCurrTeam().getExtend() < 3;
        boolean shengMing = currShengmin > enemyShengmin*3 && params.getEnemyTeam().getGlod() == 0;
        boolean isAllTrue = !isM3 && haveResource || shengMing;
        if(isAllTrue){
            List<Position> m3Pos = mapService.findByMapEnum(params.getView(),0,params.getView().getRowLen()-1,0,params.getView().getColLen()-1, MapEnum.M3);
            if(!m3Pos.isEmpty()){
                target = m3Pos.get(0);
            }
        }
        return target;
    }

    /**
     *
     * @param leader
     * @param currPos 当前位置
     * @param nextPos 下一步位置
     * @param finalPos 终点
     */
    public void buildLeaderAction(Leader leader, Position currPos, Position nextPos,Position finalPos) {
        int rowDiff = finalPos.getRowIndex()-currPos.getRowIndex();
        int colDiff = finalPos.getColIndex()-currPos.getColIndex();
        leader.setCurrPos(new Position(nextPos.getRowIndex(),nextPos.getColIndex()));
    }

    /**
     *
     * @param params
     * @param actions
     */
    public void buildLeader(GlobalValues params, List<Action> actions, Position nextPos) {

        if(nextPos == null){
            return;
        }

        Position leaderPos = params.getSessionData().getLeader().getCurrPos();
        Position finalPos = params.getSessionData().getLeader().getFinalPos();
        int sourceRowIndex = leaderPos.getRowIndex();
        int sourceColIndex = leaderPos.getColIndex();

        this.buildLeaderAction(params.getSessionData().getLeader(),leaderPos,nextPos,finalPos);

        String mId = params.getView().getMap().get(nextPos.getRowIndex()).get(nextPos.getColIndex());
        //前进的路上有自己家的坦克，让他们先走
        if(params.getCurrTeamTId().contains(mId)){
            Action action = actions.stream().filter(item->item.getTId().equals(mId)).findFirst().orElse(null);
            moveService.buildMove(params,action);
        }


        //判断移动后队友是否能跟上，不能跟上就不动
        /*boolean flag = true;
        for(Action action : actions){
            int suffix = Integer.valueOf(action.getTId().substring(1,2));

            Position itemTagetPos = null;
            switch (suffix){
                case 1:itemTagetPos = moveService.byLeader1(params);break;
                case 2:itemTagetPos = moveService.byLeader2(params);break;
                case 5:itemTagetPos = moveService.byLeader5(params);break;
            }
            mapService.buildPosition(params,itemTagetPos);
            if(itemTagetPos == null){
                continue;
            }
            Position itemCurrPos = mapService.getPosition(params.getView(),action.getTId());

            if(itemCurrPos == null){
                continue;
            }
            AStar aStar = new AStar(params.getView());
            aStar.appendBlockList(action.getTId());
            Integer step = aStar.countStep(itemCurrPos,itemTagetPos);

            if(step <= 1){
                continue;
            }
            action.setTarget(itemTagetPos);
            flag = false;
        }

        if(!flag){
            params.getSessionData().getLeader().getCurrPos().setRowIndex(sourceRowIndex);
            params.getSessionData().getLeader().getCurrPos().setColIndex(sourceColIndex);
        }*/
    }

}
