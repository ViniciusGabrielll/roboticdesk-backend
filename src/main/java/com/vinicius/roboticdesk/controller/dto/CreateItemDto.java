package com.vinicius.roboticdesk.controller.dto;

import com.vinicius.roboticdesk.entities.ItemPriority;

import java.util.List;

public record CreateItemDto(String title, ItemPriority priority, List<Long> positionsId) {
}
