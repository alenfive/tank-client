package com.source3g.tankclient.service;

import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.utils.AStar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GlodService {

    @Autowired
    private MapService mapService;

    @Autowired
    private AttackService attackService;

    @Autowired
    private MoveService moveService;

    public Position buildGlodPos(GlobalValues params, Tank tank, Position currPos) {
        int startR = currPos.getRowIndex()-tank.getShiye();
        int endR = currPos.getRowIndex()+tank.getShiye();
        int startC = currPos.getColIndex()-tank.getShiye();
        int endC = currPos.getColIndex()+tank.getShiye();

        //可视范围内存在复活币
        List<Position> positions = mapService.findByMapEnum(params.getView(),startR,endR,startC,endC, MapEnum.M2);

        return positions.isEmpty()?null:positions.get(0);
    }

    public void buildGlodMove(GlobalValues params, Action action, Tank currTank, Position currPos, Position glodPos) {
        AStar aStar = new AStar(params.getView());

        //获取最大行进路线
        Position nextPos = aStar.findPath(currPos,glodPos);

        //没有下一步就不动
        if(nextPos == null || nextPos.getParent() == null)return;

        nextPos = mapService.getMaxNext(currTank,currPos,nextPos);

        //根据坐标，计算方位和步长
        moveService.buildAction(params,action,currPos,nextPos);
    }
}
