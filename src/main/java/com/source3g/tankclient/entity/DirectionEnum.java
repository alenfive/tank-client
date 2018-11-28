package com.source3g.tankclient.entity;

public enum DirectionEnum {
    UP,RIGHT,DOWN,LEFT,WAIT;

    public static DirectionEnum getByIndex(int floor) {
        return DirectionEnum.values()[floor];
    }
}
