package com.source3g.tankclient.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.service.MainService;
import com.source3g.tankclient.service.MapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final String SESSION_KEY = "TANK_CLIENT_SESSION_KEY";

    private static SessionData sessionData = new SessionData();

    @PostMapping("/init")
    public void init(@RequestBody ClientParam clientParam) throws Exception {
        log.info("info:{}",clientParam);

        //结果时间为5分钟
        sessionData.setGameOverTime(new Date(System.currentTimeMillis()+5*60*1000));

        TeamDetail currTeam = objectMapper.readValue(objectMapper.writeValueAsString("tB".equals(clientParam.getTeam())?clientParam.getTB():clientParam.getTC()), TeamDetail.class);
        List<TankPosition> tankPositions = currTeam.getTanks().stream().map(item->{
            Position pos = mapService.getPosition(clientParam.getView(),item.getTId());
            return TankPosition.builder().tId(item.getTId()).position(pos).build();
        }).collect(Collectors.toList());

        sessionData.setTankPositions(tankPositions);

        mapService.log(clientParam.getView());
        mainService.init(clientParam);
    }


    @PostMapping("/action")
    public synchronized List<Action> action(@RequestBody ClientParam clientParam) throws Exception {
        log.info("info:{}",clientParam);
        mapService.log(clientParam.getView());

        if(sessionData.getGameOverTime() == null){
            this.init(clientParam);
        }

        clientParam.setSessionData(sessionData);
        List<Action> actions = mainService.action(clientParam);

        actions.stream().filter(item->item.getLength()>0).forEach(item-> System.out.println(item.getTId()+":"+item.getLength()+":"+item.getDirection()));
        log.info("actions:{}",actions);
        return actions;
    }


}
