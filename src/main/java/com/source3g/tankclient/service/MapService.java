package com.source3g.tankclient.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.source3g.tankclient.entity.MapEnum;
import com.source3g.tankclient.entity.Position;
import com.source3g.tankclient.entity.TMap;
import com.source3g.tankclient.entity.Tank;
import com.wangxiaobao.common.exception.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MapService {

    private List<TMap> tMaps = null;

    @Value("${map.path}")
    private String mapPath;

    @Autowired
    private ObjectMapper objectMapper;

    public void init(){
        File file = new File(mapPath);

        if(!file.exists() || !file.isDirectory()){
            throw new BizException("10006",mapPath+" 不存在，或不是一个目录");
        }

        File[] mapFiles = file.listFiles();

        List<TMap> tMaps = new ArrayList<>();
        for(File fileItem : mapFiles){
            buildTMap(tMaps,fileItem);
        }

        this.tMaps = tMaps;
    }

    private void buildTMap(List<TMap> tMaps, File fileItem) {
        List<List<String>> map = new ArrayList<>();
        BufferedReader fileReader = null;
        try{
            fileReader = new BufferedReader(new FileReader(fileItem));
            String line = null;
            Integer rowLen = 0;
            Integer colLen = 0;
            while(!StringUtils.isEmpty((line = fileReader.readLine()))){
                map.add(Arrays.asList(line.split(",")));
                rowLen ++;
                if(colLen == 0){
                    colLen = line.split(",").length;
                }
            }



            tMaps.add(TMap.builder()
                    .name(fileItem.getName())
                    .colLen(colLen)
                    .rowLen(rowLen)
                    .map(map)
                    .build());
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<TMap> getMaps() {
        if(this.tMaps == null){
            init();
        }

        return this.tMaps;
    }

    public TMap getMap(String mapName) {
        if(this.tMaps == null){
            init();
        }
        TMap tMap = this.tMaps.stream().filter(item->mapName.equals(item.getName())).findFirst().orElse(null);

        Assert.notNull(tMap,"未知地图");

        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(tMap),TMap.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 打印二维数据
     * @param view
     */
    public void log(TMap view) {
        for(int i=0;i<view.getRowLen();i++){
            for(int k=0;k<view.getColLen();k++){
                if(k>0){
                    System.out.print(",");
                }
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
        for(int i=0;i<view.getRowLen();i++){
            for(int k=0;k<view.getColLen();k++){
                if(k>0){
                    System.out.print(",");
                }
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
}
