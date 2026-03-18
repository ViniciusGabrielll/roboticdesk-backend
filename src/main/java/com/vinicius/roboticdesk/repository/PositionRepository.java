package com.vinicius.roboticdesk.repository;

import com.vinicius.roboticdesk.entities.Item;
import com.vinicius.roboticdesk.entities.Position;
import com.vinicius.roboticdesk.entities.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    void deleteByTeamId(Long teamId);


    List<Position> findAllByPositionIdInAndTeam(List<Long> longs, Team team);
}
