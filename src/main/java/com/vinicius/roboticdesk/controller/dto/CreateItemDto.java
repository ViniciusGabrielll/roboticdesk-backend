package com.vinicius.roboticdesk.controller.dto;

import java.util.List;

public record CreateItemDto(String title, Integer priority, List<Long> positionsId) {
}
