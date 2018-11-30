package com.source3g.tankclient.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.source3g.tankclient.entity.Position;
import com.source3g.tankclient.entity.TMap;
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
}
