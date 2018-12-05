package com.source3g.tankclient.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.service.MainService;
import com.source3g.tankclient.service.MapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/player")
@Slf4j
public class ClientController {

    @Autowired
    private MainService mainService;

    @Autowired
    private MapService mapService;

    @Autowired
    private ObjectMapper objectMapper;

    private static SessionData sessionData;

    @PostMapping("/init")
    public void init(@RequestBody ClientParam clientParam) throws Exception {
        log.info("info:{}",clientParam);
        sessionData = new SessionData();

        //结果时间为5分钟
        sessionData.setGameOverTime(new Date(System.currentTimeMillis()+5*60*1000));

        TeamDetail currTeam = objectMapper.readValue(objectMapper.writeValueAsString("tB".equals(clientParam.getTeam())?clientParam.getTB():clientParam.getTC()), TeamDetail.class);
        List<TankPosition> tankPositions = currTeam.getTanks().stream().map(item->{
            Position pos = mapService.getPosition(clientParam.getView(),item.getTId());

            Assert.notNull(pos,"坦克信息不全，重新开始游戏");

            return TankPosition.builder().tId(item.getTId()).position(pos).build();
        }).collect(Collectors.toList());

        Position leaderPos = buildLeaderPos(clientParam.getView(),currTeam);
        sessionData.setLeader(Leader.builder().pos(leaderPos).build());
        sessionData.setTankPositions(tankPositions);

        mapService.log(clientParam.getView());
        mainService.init(clientParam);
    }

    private Position buildLeaderPos(TMap view,TeamDetail currTeam) {
        for(Tank tank : currTeam.getTanks()){
            int suffix = Integer.valueOf(tank.getTId().substring(1,2));
            if(suffix == 2){
                return mapService.getPosition(view,tank.getTId());
            }
        }
        return null;
    }


    @PostMapping("/action")
    public synchronized List<Action> action(@RequestBody ClientParam clientParam) throws Exception {
        log.info("info:{}",clientParam);
        mapService.log(clientParam.getView());

        if(sessionData == null || sessionData.getGameOverTime() == null){
            this.init(clientParam);
        }
        sessionData.getLeader().setDirection(null);
        clientParam.setSessionData(sessionData);
        List<Action> actions = mainService.action(clientParam);

        actions.stream().filter(item->item.getLength()>0).forEach(item-> System.out.println(item.getTId()+":"+item.getLength()+":"+item.getDirection()+":"+item.getType()));
        log.info("actions:{}",actions);
        return actions;
    }


}
