package com.source3g.tankclient.action;

import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.service.MapService;
import com.source3g.tankclient.utils.AStar;
import com.source3g.tankclient.utils.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 攻击BOSS
 */
@SuppressWarnings("Duplicates")
@Component
public class AttackBossAction extends AbstractActiion<GlobalValues,Action> {

    @Autowired
    private MapService mapService;

    @Override
    public NodeType process(GlobalValues params, Action action) {

        TMap view = params.getView();
        Position currPos = MapUtils.getPosition(params.getView(),action.getTId());

        Tank tank = params.currTeam.getTanks().stream().filter(item->item.getTId().equals(action.getTId())).findFirst().orElse(null);
        //发现BOSS
        List<Position> positions = mapService.findByMapEnum(view,0,view.getRowLen()-1,0,view.getColLen()-1,MapEnum.A1);
        if(positions.isEmpty()){
            return NodeType.Failure;
        }


        //在攻击范围内
        if(ableAttack(tank,currPos,positions.get(0))){
            Tank boss = params.bossTeam.getTanks().stream().filter(item->MapEnum.A1.name().equals(item.getTId())).findFirst().orElse(null);
            attackTank(view,tank,action,currPos,positions.get(0),boss);
            return NodeType.Success;
        }

        //制定路线
        AStar aStar = new AStar(view);
        int startR = positions.get(0).getRowIndex()-tank.getShecheng();
        int endR = positions.get(0).getRowIndex()+tank.getShecheng();
        int startC = positions.get(0).getColIndex()-tank.getShecheng();
        int endC = positions.get(0).getColIndex()+tank.getShecheng();

        //BOSS周围的可选位置
        List<Position> ablePos = mapService.findByMapEnum(view,startR,endR,startC,endC,MapEnum.M1,MapEnum.M2,MapEnum.M3);
        ablePos = ablePos.stream().filter(item->item.getRowIndex()==positions.get(0).getRowIndex() || item.getColIndex() == positions.get(0).getColIndex()).collect(Collectors.toList());

        if(ablePos.size() == 0){
            return NodeType.Failure;
        }

        //计算距离当前位置最近的点
        Position nextTempPos = null;
        int minStep = 0;
        for(int i=0;i<ablePos.size();i++){
            aStar.clear();
            int step = aStar.countStep(currPos,ablePos.get(i));
            if(nextTempPos == null || minStep>step){
                nextTempPos = ablePos.get(i);
                minStep = step;
            }
        }

        //寻路
        aStar.clear();
        Position nextPos = aStar.findPath(currPos,nextTempPos);
        //获取最大行进路线
        nextPos = mapService.getMaxNext(tank,currPos,nextPos);

        //允许下一步，替换地图
        params.getView().getMap().get(currPos.getRowIndex()).set(currPos.getColIndex(),MapEnum.M1.name());
        params.getView().getMap().get(nextPos.getRowIndex()).set(nextPos.getColIndex(),action.getTId());

        //根据坐标，计算方位和步长
        buildAction(action,currPos,nextPos);
        return NodeType.Success;
    }

    /**
     * 攻击
     * @param view
     * @param tank
     * @param action
     * @param currPos
     * @param bossPos
     * @param boss
     */
    private void attackTank(TMap view, Tank tank, Action action, Position currPos, Position bossPos, Tank boss) {
        DirectionEnum direct = DirectionEnum.UP;
        if(currPos.getRowIndex()<bossPos.getRowIndex()){
            direct = DirectionEnum.DOWN;
        }else if(currPos.getColIndex()<bossPos.getColIndex()){
            direct = DirectionEnum.RIGHT;
        }else if(currPos.getColIndex()>bossPos.getColIndex()){
            direct = DirectionEnum.LEFT;
        }
        int diff = Math.abs(bossPos.getRowIndex()-currPos.getRowIndex())+Math.abs(bossPos.getColIndex()-currPos.getColIndex());
        action.setDirection(direct);
        action.setType(ActionTypeEnum.FIRE);
        action.setLength(diff);

        //打死BOSS后地图上移除
        if(boss.getShengyushengming()-tank.getGongji() <=0){
            view.getMap().get(bossPos.getRowIndex()).set(bossPos.getColIndex(),MapEnum.M1.name());
        }

    }

    /**
     * 可攻击范围
     * @param tank
     * @param currPos
     * @param targetPos
     * @return
     */
    private boolean ableAttack(Tank tank, Position currPos, Position targetPos) {
        int diff = Math.abs(targetPos.getRowIndex()-currPos.getRowIndex())+Math.abs(targetPos.getColIndex()-currPos.getColIndex());
        return (currPos.getRowIndex()==targetPos.getRowIndex() || currPos.getColIndex() == targetPos.getColIndex()) && diff<=tank.getShecheng();
    }

    private void buildAction(Action action, Position currPos, Position nextPos) {
        int rowDiff = nextPos.getRowIndex()-currPos.getRowIndex();
        int colDiff = nextPos.getColIndex()-currPos.getColIndex();
        action.setDirection(rowDiff>0?DirectionEnum.DOWN:rowDiff<0?DirectionEnum.UP:colDiff>0?DirectionEnum.RIGHT:colDiff<0?DirectionEnum.LEFT:DirectionEnum.WAIT);
        action.setLength(Math.abs(rowDiff!=0?rowDiff:colDiff));
        action.setType(ActionTypeEnum.MOVE);
    }

}
