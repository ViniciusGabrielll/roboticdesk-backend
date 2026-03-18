package com.vinicius.roboticdesk.controller;

import com.vinicius.roboticdesk.controller.dto.CreatePositionDto;
import com.vinicius.roboticdesk.entities.*;
import com.vinicius.roboticdesk.repository.*;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/positions")
public class PositionController {

    private final PositionRepository positionRepository;

    private final TeamRepository teamRepository;

    private final RoleRepository roleRepository;

    private final UserRepository userRepository;

    private final TeamInviteRepository teamInviteRepository;

    private final ItemRepository itemRepository;

    @GetMapping("/teams/{teamId}")
    public ResponseEntity<List<Position>> listPositions(@PathVariable Long teamId,
                                                        @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuário não encontrado"
                ));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Time não encontrado"
                ));

        if (user.getTeam() == null || !user.getTeam().getId().equals(team.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Você não pertence a esse time"
            );
        }

        return ResponseEntity.ok(team.getPositions());
    }

    @Transactional
    @PostMapping("/teams/{teamId}")
    public ResponseEntity<Void> createPosition(@RequestBody CreatePositionDto dto, @PathVariable Long teamId,
                                               @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuário não encontrado"
                ));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Time não encontrado"
                ));

        if (user.getTeam() == null || !user.getTeam().getId().equals(team.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Você não pertence a esse time"
            );
        }

        Position position = new Position();
        position.setPositionName(dto.positionName());
        position.setTeam(team);
        position.setColor(dto.color());
        positionRepository.save(position);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Transactional
    @DeleteMapping("/{positionId}")
    public ResponseEntity<Void> deletePosition(@PathVariable Long positionId,
                                                        @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuário não encontrado"
                ));


        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Position não encontrado"
                ));

        if(!user.getTeam().getPositions().contains(position)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Essa position não pertence ao seu time.");
        }

        for (User u : position.getUsers()) {
            u.getPositions().remove(position);
        }

        positionRepository.delete(position);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('admin') or hasRole('scrummaster')")
    @Transactional
    @PostMapping("/teams/{teamId}/users/{memberId}/{positionId}")
    public ResponseEntity<Void> assignPositionUser(@PathVariable String memberId, @PathVariable Long positionId, @PathVariable Long teamId, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID memberUUID = UUID.fromString(memberId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuário não encontrado"
                ));

        User member = userRepository.findById(memberUUID)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Membro não encontrado"
                ));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Time não encontrado"
                ));

        if (user.getTeam() == null || !user.getTeam().getId().equals(team.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Você não pertence a esse time"
            );
        }

        if (member.getTeam() == null || !member.getTeam().getId().equals(team.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "O membro selecionado não pertence a esse time"
            );
        }

        Position position = positionRepository.findById(positionId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Posição não encontrada"));

        List<Position> positions = member.getPositions();
        if (positions == null) {
            positions = new ArrayList<>();
        }
        if (!positions.contains(position)) {
            positions.add(position);
        }
        member.setPositions(positions);
        userRepository.save(member);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PreAuthorize("hasRole('admin') or hasRole('scrummaster')")
    @Transactional
    @DeleteMapping("/users/{memberId}/{positionId}")
    public ResponseEntity<Void> removePositionUser(@PathVariable String memberId, @PathVariable Long positionId, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID memberUUID = UUID.fromString(memberId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuário não encontrado"
                ));

        User member = userRepository.findById(memberUUID)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Membro não encontrado"
                ));

        Position position = positionRepository.findById(positionId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Posição não encontrada"));

        if(!user.getTeam().equals(member.getTeam())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "O membro selecionado não pertence a o seu time"
            );
        }

        if(!user.getTeam().equals(position.getTeam())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "A position selecionada não pertence ao seu time"
            );
        }

        member.getPositions().remove(position);

        userRepository.save(member);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Transactional
    @PostMapping("/teams/{teamId}/items/{itemId}/{positionId}")
    public ResponseEntity<Void> assignPositionItem(@PathVariable Long itemId, @PathVariable Long positionId, @PathVariable Long teamId, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuário não encontrado"
                ));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Item não encontrado"
                ));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Time não encontrado"
                ));

        if (user.getTeam() == null || !user.getTeam().getId().equals(team.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Você não pertence a esse time"
            );
        }

        if (item.getTeam() == null || !item.getTeam().getId().equals(team.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "O item selecionado não pertence a esse time"
            );
        }

        Position position = positionRepository.findById(positionId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Posição não encontrada"));

        List<Position> positions = item.getPositions();
        if (positions == null) {
            positions = new ArrayList<>();
        }
        if (!positions.contains(position)) {
            positions.add(position);
        }
        item.setPositions(positions);
        itemRepository.save(item);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
