package com.source3g.tankclient;

import com.source3g.tankclient.entity.Position;
import com.source3g.tankclient.entity.TMap;
import com.source3g.tankclient.service.MapService;
import com.source3g.tankclient.utils.AStar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class TankClientApplication implements CommandLineRunner{

	public static void main(String[] args) {

		SpringApplication.run(TankClientApplication.class, args);
	}

	@Autowired
	private MapService mapService;

	@Override
	public void run(String... strings) {
		TMap tMap = mapService.getMap("32X24");
		AStar aStar = new AStar(tMap);
		Position currPos = new Position(4,10);
		Position targetPos = new Position(4,12);
		mapService.log(tMap);
		Position nextPos = aStar.findPath(currPos,targetPos);
		while (nextPos != null){
			log.info("path:row:{},col:{}",nextPos.getRowIndex(),nextPos.getColIndex());
			nextPos = nextPos.getParent();
		}

	}
}
