package com.source3g.tankclient.service;

import com.source3g.tankclient.entity.TMap;
import org.springframework.stereotype.Service;

@Service
public class MapService {


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
}
