package com.vinicius.roboticdesk.repository;

import com.vinicius.roboticdesk.entities.Sprint;
import com.vinicius.roboticdesk.entities.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {
    void deleteByTeam(Team team);
}
