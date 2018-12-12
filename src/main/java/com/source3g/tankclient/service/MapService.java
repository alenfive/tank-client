package com.source3g.tankclient.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.utils.AStar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("Duplicates")
@Service
public class MapService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AttackService attackService;

    private final List<String> blocks = Arrays.asList(
            MapEnum.M1.name(),
            MapEnum.M2.name(),
            MapEnum.M3.name());


    private final List<String> blockTrues = Arrays.asList(
            MapEnum.M4.name(),
            MapEnum.M5.name(),
            MapEnum.M6.name(),
            MapEnum.M7.name(),
            MapEnum.M8.name());

    /**
     * 打印二维数据
     * @param view
     */
    public void log(TMap view) {

        System.out.print(String.format("%2s", "") );
        for(int c=0;c<view.getColLen();c++){
            System.out.print(String.format("%4s", c));
        }

        System.out.println();

        for(int i=0;i<view.getRowLen();i++){
            for(int k=0;k<view.getColLen();k++){
                if(k == 0){
                    System.out.print(String.format("%2s", i));
                }
                System.out.print("  ");
                System.out.print(view.getMap().get(i).get(k));
            }
            System.out.println("");
        }

    }

    /**
     * 标记地图区域中心点
     * @param view
     * @param regions
     * @param flag
     */
    public void flag(TMap view, List<Position> regions, String flag) {
        System.out.println("--------------------------------------------------------------");
        System.out.print(String.format("%2s", "") );
        for(int c=0;c<view.getColLen();c++){
            System.out.print(String.format("%4s", c));
        }
        System.out.println();
        for(int i=0;i<view.getRowLen();i++){
            for(int k=0;k<view.getColLen();k++){
                if(k == 0){
                    System.out.print(String.format("%2s", i));
                }
                System.out.print("  ");
                int finalI = i;
                int finalK = k;
                Position pos = regions.stream().filter(item->item.getRowIndex() == finalI && item.getColIndex() == finalK).findFirst().orElse(null);
                if(pos != null){
                    System.out.print(flag);
                }else{
                    System.out.print(view.getMap().get(i).get(k));
                }
            }
            System.out.println("");
        }
    }

    /**
     * 根据元素查询范围内坐标
     * @param view
     * @param startR
     * @param endR
     * @param startC
     * @param endC
     * @param mapEnum
     * @return
     */
    public List<Position> findByMapEnum(TMap view,int startR, int endR, int startC, int endC, MapEnum ... mapEnum){

        startR = startR<0?0:startR;
        endR = endR>=view.getRowLen()?view.getRowLen()-1:endR;
        startC = startC<0?0:startC;
        endC = endC>=view.getColLen()?view.getColLen()-1:endC;

        List<Position> positions = new ArrayList<>();
        for(int i=startR;i<=endR;i++){
            for(int k=startC;k<=endC;k++){
                if(contain(view,mapEnum,i,k)){
                    positions.add(new Position(i,k));
                }
            }
        }
        return positions;
    }

    /**
     * 某坐标是否包含某些地图元素
     * @param view
     * @param mapEnum
     * @param i
     * @param k
     * @return
     */
    private boolean contain(TMap view, MapEnum[] mapEnum, int i, int k) {
        for (MapEnum item : mapEnum){
            if(item.name().equals(view.getMap().get(i).get(k))){
                return true;
            }
        }
        return false;
    }

    /**
     * 获取最大行进路线
     * @param tank
     * @param nextPos
     * @return
     */
    public Position getMaxNext(Tank tank, Position currPos, Position nextPos) {
        if(nextPos == null || nextPos.getParent() == null)return null;

        int yidong = 0;

        while (nextPos.getParent() != null && yidong<tank.getYidong()){
            //判断是否是直线路径
            if(nextPos.getParent().getColIndex() != currPos.getColIndex() &&
                    nextPos.getParent().getRowIndex() != currPos.getRowIndex()){
                break;
            }
            nextPos = nextPos.getParent();
            yidong ++;
        }
        return nextPos;
    }

    /**
     * 获取某元素坦克
     * @param view
     * @param tId
     * @return
     */
    public Position getPosition(TMap view, String tId) {
        for(int r=0;r<view.getRowLen();r++){
            for(int c=0;c<view.getColLen();c++){
                if(tId.equals(view.getMap().get(r).get(c))){
                    return new Position(r,c);
                }
            }
        }
        return null;
    }

    public List<Position> listPosition(TMap view,List<Tank> tanks){
        List<String> tIds = tanks.stream().map(Tank::getTId).collect(Collectors.toList());
        List<Position> result = new ArrayList<>();
        for(int r=0;r<view.getRowLen();r++){
            for(int c=0;c<view.getColLen();c++){
                if(tIds.contains(view.getMap().get(r).get(c))){
                    result.add(new Position(r,c));
                }
            }
        }
        return result;
    }

    public void buildPosition(GlobalValues params, Position pos){
        if (pos == null)return;
        TMap view = params.getView();
        int rowIndex = pos.getRowIndex()<=0?0:pos.getRowIndex();
        rowIndex = rowIndex>view.getRowLen()-1?view.getRowLen()-1:rowIndex;
        int colIndex = pos.getColIndex()<=0?0:pos.getColIndex();
        colIndex = colIndex>view.getColLen()-1?view.getColLen()-1:colIndex;

        pos.setRowIndex(rowIndex);
        pos.setColIndex(colIndex);
    }

    public boolean isBlock(String mId) {
        return !blocks.contains(mId);
    }

    public boolean isBlockTrue(String mId){
        return blockTrues.contains(mId);
    }

    /**
     * 返回target附近空白点距离currPos最近的点
     * @param targetPos
     */
    public void buildBlank(GlobalValues params,Position currPos, Position targetPos) {
        this.buildPosition(params,targetPos);

        int diff = Math.abs(targetPos.getRowIndex()-currPos.getRowIndex()) + Math.abs(targetPos.getColIndex()-currPos.getColIndex());
        if(diff == 1){  //挨着的，已经最近的距离了
            return;
        }

        String mId = params.getView().getMap().get(targetPos.getRowIndex()).get(targetPos.getColIndex());
        if(!isBlock(mId)){
            return;
        }
        List<Position> ablePos = Arrays.asList(
                new Position(targetPos.getRowIndex()-1,targetPos.getColIndex()-1),
                new Position(targetPos.getRowIndex()-1,targetPos.getColIndex()+1),
                new Position(targetPos.getRowIndex()+1,targetPos.getColIndex()+1),
                new Position(targetPos.getRowIndex()+1,targetPos.getColIndex()-1),
                new Position(targetPos.getRowIndex()-1,targetPos.getColIndex()),
                new Position(targetPos.getRowIndex(),targetPos.getColIndex()+1),
                new Position(targetPos.getRowIndex()+1,targetPos.getColIndex()),
                new Position(targetPos.getRowIndex(),targetPos.getColIndex()-1)
        );

        List<Position> attackPos = attackService.enemyAttackPosList(params);

        //非障碍物并且非敌方弹道
        ablePos.forEach(item->buildPosition(params,item));
        ablePos = ablePos.stream().filter(item->{
            String itemMId = params.getView().get(item.getRowIndex(),item.getColIndex());
            return !this.isBlock(itemMId) && !attackPos.contains(item) ;
        }).collect(Collectors.toList());


        if(ablePos.isEmpty())return;

        AStar aStar = new AStar(params.getView());
        List<DiffPosition> diffPos = ablePos.stream().map(item->{
            DiffPosition diffPosition = new DiffPosition();
            diffPosition.setPos(item);
            aStar.clear();
            diffPosition.setDiff(aStar.countStep(currPos,item));
            return diffPosition;
        }).collect(Collectors.toList());

        DiffPosition minDiffPos = diffPos.stream().min(Comparator.comparing(DiffPosition::getDiff)).get();
        targetPos.setRowIndex(minDiffPos.getPos().getRowIndex());
        targetPos.setColIndex(minDiffPos.getPos().getColIndex());
    }

    public TMap copyAttackLine(GlobalValues params,String ... excludeTankIds) {
        try {
            List<String> excludeTankIdList = Arrays.asList(excludeTankIds);
            TMap result = objectMapper.readValue(objectMapper.writeValueAsString(params.getView()),TMap.class);

            //置灰敌方的攻击路线
            params.getEnemyTeam().getTanks().stream().filter(item->!excludeTankIdList.contains(item.getTId())).forEach(item->{

                Position itemPos = this.getPosition(result,item.getTId());
                if(itemPos == null)return;

                List<Position> attackLine = attackService.enemyAttackPosList(params);

                attackLine.forEach(item2->{
                    String mId = result.get(item2.getRowIndex(),item2.getColIndex());
                    if(MapEnum.M1.name().equals(mId) || MapEnum.M3.name().equals(mId)){
                        result.getMap().get(item2.getRowIndex()).set(item2.getColIndex(),MapEnum.T1.name());
                    }
                });
            });


            //this.log(result);

            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isPosition(TMap view,Position itemPos) {
        return itemPos.getRowIndex()>=0 && itemPos.getRowIndex()<view.getRowLen() && itemPos.getColIndex()>=0 && itemPos.getColIndex()<view.getColLen();
    }
}
