package com.source3g.tankclient.entity;

public enum ActionTypeEnum {
    MOVE,FIRE;

    public static ActionTypeEnum getByIndex(Integer integer){
        return ActionTypeEnum.values()[integer];
    }
}
