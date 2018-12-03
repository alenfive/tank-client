package com.source3g.tankclient.service;

import com.source3g.tankclient.entity.GlobalValues;
import com.source3g.tankclient.entity.MapEnum;
import com.source3g.tankclient.entity.Position;
import com.source3g.tankclient.entity.Tank;
import com.source3g.tankclient.utils.AStar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 阵型
 */
@Service
public class FormationService {

    @Autowired
    private MapService mapService;

    /**
     * 乱枪打死
     * @param params
     * @param currTank
     * @param currPos
     * @param targetPos
     * @return
     */
    public Position random(GlobalValues params,Tank currTank,Position currPos,Position targetPos){
        int startR = targetPos.getRowIndex()-currTank.getShecheng();
        int endR = targetPos.getRowIndex()+currTank.getShecheng();
        int startC = targetPos.getColIndex()-currTank.getShecheng();
        int endC = targetPos.getColIndex()+currTank.getShecheng();

        List<Position> ablePos = mapService.findByMapEnum(params.getView(),startR,endR,startC,endC,MapEnum.M1,MapEnum.M2,MapEnum.M3);
        ablePos = ablePos.stream().filter(item->item.getRowIndex()==targetPos.getRowIndex() || item.getColIndex() == targetPos.getColIndex()).collect(Collectors.toList());
        if(ablePos.isEmpty())return null;

        AStar aStar = new AStar(params.getView());
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
        nextPos = mapService.getMaxNext(currTank,currPos,nextPos);

        return nextPos;
    }

    /**
     * 菊花阵
     * @param params
     * @param tank
     * @return
     */
    public Position plumBlossom(GlobalValues params, Tank tank){
        return null;
    }

    /**
     * 长蛇阵
     * @param params
     * @param tank
     * @return
     */
    public Position longSnake(GlobalValues params, Tank tank){
        return null;
    }

    /**
     * 三才阵
     * @param params
     * @param tank
     * @return
     */
    public Position threeTalents(GlobalValues params,Tank tank,Position currPos,Position targetPos){
        Tank maxTank = params.getCurrTeam().getTanks().stream().max((a,b)->a.getShengming()>b.getShengming()?-1:1).get();
        Position maxShengMing = mapService.getPosition(params.getView(),maxTank.getTId());
        if(maxShengMing == null)return null;

        return null;
    }

    /**
     * 获得集结点
     * @param params
     * @return
     */
    public Position getMass(GlobalValues params){
        List<Tank> tanks = params.getCurrTeam().getTanks().stream().filter(item->item.getShengyushengming()>0).collect(Collectors.toList());
        Tank maxShengMing = tanks.stream().max(Comparator.comparingInt(Tank::getShengming)).get();
        if(maxShengMing == null)return null;
        return mapService.getPosition(params.getView(),maxShengMing.getTId());

    }
}
