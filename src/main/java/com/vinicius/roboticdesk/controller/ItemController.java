package com.vinicius.roboticdesk.controller;

import com.vinicius.roboticdesk.controller.dto.CreateItemDto;
import com.vinicius.roboticdesk.entities.*;
import com.vinicius.roboticdesk.repository.ItemRepository;
import com.vinicius.roboticdesk.repository.PositionRepository;
import com.vinicius.roboticdesk.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/items")
public class ItemController {

    private final ItemRepository itemRepository;

    private final UserRepository userRepository;

    private final PositionRepository positionRepository;

    @Transactional
    @PostMapping
    public ResponseEntity<Void> createItem(@RequestBody CreateItemDto dto, @AuthenticationPrincipal Jwt
            jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuário não encontrado"
                ));

        var item = new Item();
        item.setTitle(dto.title());
        item.setTeam(user.getTeam());
        item.setPriority(dto.priority());
        item.setStatus(ItemStatus.TODO);
        List<Position> positions = positionRepository
                .findAllByPositionIdInAndTeam(dto.positionsId(), user.getTeam());

        if (positions.size() != dto.positionsId().size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Uma ou mais positions não pertencem ao time ou não existem"
            );
        }

        item.setPositions(positions);
        itemRepository.save(item);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Transactional
    @GetMapping
    public ResponseEntity<List<Item>> listItems(@AuthenticationPrincipal Jwt
            jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuário não encontrado"
                ));

        if(user.getTeam() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário não pertence a um time");
        }

        var team = new Team();
        team = user.getTeam();

        var items = team.getItems();

        return ResponseEntity.ok(items);
    }

    @Transactional
    @PatchMapping("{itemId}/status/{statusName}")
    public ResponseEntity<Void> changeStatus(@AuthenticationPrincipal Jwt
                                                        jwt, @PathVariable Long itemId,@PathVariable String statusName) {
        UUID userId = UUID.fromString(jwt.getSubject());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuário não encontrado"
                ));

        if(user.getTeam() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário não pertence a um time");
        }

        var team = user.getTeam();

        Item item = itemRepository.findById(itemId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Item não encontrado"
        ));
        if(!item.getTeam().getId().equals(team.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Você não pertence ao time do item");
        }

        ItemStatus status;
        try {
            status = ItemStatus.valueOf(statusName);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status inexistente!");
        }

        item.setStatus(status);
        itemRepository.save(item);


        return ResponseEntity.ok().build();
    }

    @Transactional
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long itemId, @AuthenticationPrincipal Jwt
            jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuário não encontrado"
                ));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Item não encontrado"
                ));

        if(!user.getTeam().getItems().contains(item)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esse item não pertence ao seu time.");
        }

        itemRepository.delete(item);

        return ResponseEntity.noContent().build();
    }
}
