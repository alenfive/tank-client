package com.source3g.tankclient.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionData {
    //是否集结
    private boolean isMass;

    //游戏结束倒计时
    private Date gameOverTime;

    //坦克最后的移动位置
    private List<TankPosition> tankLastPosList;

    //坦克初始化位置
    private List<TankPosition> tankInitPosList;
}
