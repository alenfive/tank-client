package com.source3g.tankclient.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.source3g.tankclient.entity.*;
import com.source3g.tankclient.service.MainService;
import com.source3g.tankclient.service.MapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
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


    @GetMapping("/systemInit")
    public void systemInitUrl(){
        log.info("初始化成功");
    }

    @PostMapping("/init")
    public void init(@RequestBody ClientParam clientParam) throws Exception {
        log.info("info:{}",clientParam);
        sessionData = new SessionData();

        //结果时间为5分钟
        sessionData.setGameOverTime(new Date(System.currentTimeMillis()+5*60*1000));

        TeamDetail currTeam = objectMapper.readValue(objectMapper.writeValueAsString("tB".equals(clientParam.getTeam())?clientParam.getTB():clientParam.getTC()), TeamDetail.class);
        List<TankPosition> tankInitPosList = currTeam.getTanks().stream().map(item->{
            Position pos = mapService.getPosition(clientParam.getView(),item.getTId());

            Assert.notNull(pos,"坦克信息不全，重新开始游戏");

            return TankPosition.builder().tId(item.getTId()).tank(item).pos(pos).build();
        }).collect(Collectors.toList());

        sessionData.setTankLastPosList(objectMapper.readValue(objectMapper.writeValueAsBytes(tankInitPosList),new TypeReference<List<TankPosition>>(){}));
        sessionData.setTankInitPosList(objectMapper.readValue(objectMapper.writeValueAsBytes(tankInitPosList),new TypeReference<List<TankPosition>>(){}));

        mapService.log(clientParam.getView());
        mainService.init(clientParam);
    }

    @PostMapping("/action")
    public synchronized List<Action> action(@RequestBody ClientParam clientParam) throws Exception {

        log.info("info:{}",clientParam);
        mapService.log(clientParam.getView());

        if(sessionData == null || sessionData.getGameOverTime() == null){
            this.init(clientParam);
        }
        clientParam.setSessionData(sessionData);
        List<Action> actions = mainService.action(clientParam);

        actions.stream().filter(item->item.getLength()>0).forEach(item-> System.out.println(item.getTId()+":"+item.getLength()+":"+item.getDirection()+":"+item.getType()+":"+item.getSort()));
        log.info("actions:{}",actions);
        return actions;
    }


}
