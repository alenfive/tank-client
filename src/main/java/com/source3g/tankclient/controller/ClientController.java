package com.source3g.tankclient.controller;

import com.source3g.tankclient.entity.Action;
import com.source3g.tankclient.entity.ClientParam;
import com.source3g.tankclient.entity.SessionData;
import com.source3g.tankclient.service.MainService;
import com.source3g.tankclient.service.MapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/player")
@Slf4j
public class ClientController {

    @Autowired
    private MainService mainService;

    @Autowired
    private MapService mapService;

    private final String SESSION_KEY = "TANK_CLIENT_SESSION_KEY";

    @PostMapping("/init")
    public void init(@RequestBody ClientParam clientParam, HttpSession session) throws Exception {
        log.info("info:{}",clientParam);
        session.setAttribute(SESSION_KEY,new SessionData());
        mapService.log(clientParam.getView());
        mainService.init(clientParam);
    }


    @PostMapping("/action")
    public synchronized List<Action> action(@RequestBody ClientParam clientParam, HttpSession session) throws IOException {
        log.info("info:{}",clientParam);
        mapService.log(clientParam.getView());

        SessionData sessionData = (SessionData) session.getAttribute("SESSION_KEY");
        if(sessionData == null){
            sessionData = new SessionData();
        }

        clientParam.setSessionData(sessionData);
        List<Action> actions = mainService.action(clientParam);

        actions.stream().filter(item->item.getLength()>0).forEach(item-> System.out.println(item.getTId()+":"+item.getLength()+":"+item.getDirection()));
        log.info("actions:{}",actions);
        return actions;
    }


}
