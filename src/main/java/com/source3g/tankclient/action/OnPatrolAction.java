package com.source3g.tankclient.action;

import com.source3g.tankclient.entity.Action;
import com.source3g.tankclient.entity.GlobalValues;
import com.source3g.tankclient.entity.MapEnum;
import com.source3g.tankclient.entity.Position;
import com.source3g.tankclient.service.MapService;
import com.source3g.tankclient.service.MoveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 巡逻
 */
@Component
public class OnPatrolAction extends AbstractActiion<GlobalValues,List<Action>> {

    @Autowired
    private MapService mapService;
    @Autowired
    private MoveService moveService;

    @Override
    public NodeType process(GlobalValues params, List<Action> actions) {

        if(params.getSessionData().getLeader().getDirection() == null){
            //构建leader行动
            Position targetPos = buildCenterBlank(params);
            moveService.buildLeader(params,actions,targetPos);
        }

        for(Action action : actions){
            moveService.buildMove(params,action);
        }

        return NodeType.Success;
    }






    //获得中心的某个空白点
    private Position buildCenterBlank(GlobalValues params) {

        Position target = new Position(params.getView().getRowLen()/2,params.getView().getColLen()/2);
        String centerMId = params.getView().get(target.getRowIndex(),target.getColIndex());
        Integer currShengmin = params.getCurrTeam().getTanks().stream().map(item->item.getShengyushengming()).reduce(Integer::sum).get();
        Integer enemyShengmin = params.getEnemyTeam().getTanks().stream().map(item->item.getShengyushengming()).reduce(Integer::sum).get();

        //还有资源，或者敌方相对残血时，继续扫描未知区域
        if(!MapEnum.M3.name().equals(centerMId) && params.getEnemyTeam().getExtend() + params.getCurrTeam().getExtend()<3 ||
                (currShengmin < enemyShengmin*3 && params.getEnemyTeam().getGlod() == 0)){
            List<Position> m3Pos = mapService.findByMapEnum(params.getView(),0,params.getView().getRowLen()-1,0,params.getView().getColLen()-1, MapEnum.M3);
            if(!m3Pos.isEmpty()){
                target = m3Pos.get(0);
            }
        }

       /*
        String mId = params.getView().getMap().get(target.getRowIndex()).get(target.getColIndex());
        if(!mapService.isBlock(mId)){
            return target;
        }

        List<Position> m3Pos = mapService.findByMapEnum(params.getView(),0,params.getView().getRowLen()-1,0,params.getView().getColLen()-1,MapEnum.M3);
        if(!m3Pos.isEmpty()){
            return m3Pos.get(0);
        }

        List<Position> m1Pos = mapService.findByMapEnum(params.getView(),params.getView().getRowLen()/4,params.getView().getRowLen()-params.getView().getRowLen()/4,params.getView().getColLen()/4,params.getView().getColLen()-params.getView().getColLen()/4,MapEnum.M1);
        if (!m1Pos.isEmpty()){
            return m1Pos.get(0);
        }*/
        return target;
    }



}
