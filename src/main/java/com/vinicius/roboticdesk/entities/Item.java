package com.vinicius.roboticdesk.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "tb_items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "title")
    private String title;

    @Enumerated(EnumType.STRING)
    private ItemPriority priority;

    @Enumerated(EnumType.STRING)
    private ItemStatus status;

    @ManyToMany
    @JoinTable(
            name = "tb_item_positions",
            joinColumns = @JoinColumn(name = "item_id", nullable = true),
            inverseJoinColumns = @JoinColumn(name = "position_id", nullable = true)
    )
    private List<Position> positions;

    @ManyToOne
    @JoinColumn(name = "team_id")
    @JsonIgnore
    private Team team;


    @ManyToOne
    @JoinColumn(name = "sprint_id", nullable = true)
    @JsonIgnore
    private Sprint sprint;

}
