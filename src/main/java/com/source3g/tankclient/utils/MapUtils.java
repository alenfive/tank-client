package com.source3g.tankclient.utils;

import com.source3g.tankclient.entity.Position;
import com.source3g.tankclient.entity.TMap;

public class MapUtils {

    public static Position getPosition(TMap view, String tId) {
        for(int r=0;r<view.getRowLen();r++){
            for(int c=0;c<view.getColLen();c++){
                if(tId.equals(view.getMap().get(r).get(c))){
                    return new Position(r,c);
                }
            }
        }
        return null;
    }

}
