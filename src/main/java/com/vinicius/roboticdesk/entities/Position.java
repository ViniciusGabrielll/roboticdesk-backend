package com.vinicius.roboticdesk.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.processing.Pattern;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "tb_positions")
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "position_id")
    private Long positionId;

    @Column(unique = true)
    private String positionName;

    @ManyToMany(mappedBy = "positions")
    @JsonIgnore
    private List<User> users;

    @ManyToMany(mappedBy = "positions")
    @JsonIgnore
    private List<Item> items;

    @ManyToOne
    @JsonIgnore
    private Team team;

    private String color;

}
