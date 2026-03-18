package com.vinicius.roboticdesk.controller;

import com.vinicius.roboticdesk.controller.dto.CreateTeamDto;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/teams")
public class TeamController {


    private final TeamRepository teamRepository;

    private final PositionRepository positionRepository;

    private final SprintRepository sprintRepository;

    private final UserRepository userRepository;

    private final TeamInviteRepository teamInviteRepository;

    private final ItemRepository itemRepository;

    @PreAuthorize("hasRole('admin') or hasRole('scrummaster')")
    @Transactional
    @PostMapping
    public ResponseEntity<Void> createTeam(@RequestBody CreateTeamDto dto, @AuthenticationPrincipal Jwt
            jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuário não encontrado"
                ));

        if(user.getTeam() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário já pertence a um time");
        }

        var team = new Team();
        team.setName(dto.name());
        teamRepository.save(team);
        user.setTeam(team);
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PreAuthorize("hasRole('admin') or hasRole('scrummaster')")
    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long id, @AuthenticationPrincipal Jwt
            jwt) {

        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Time não encontrado"
                ));

        UUID userId = UUID.fromString(jwt.getSubject());

        User userRequest = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Seu usuário não encontrado"
                ));

        List<User> users = userRepository.findAllByTeam(team);

        boolean isAdmin = userRequest.getRoles()
                .stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("ADMIN"));

        boolean belongsToTeam = userRequest.getTeam() != null
                && userRequest.getTeam().getId().equals(team.getId());

        if (!belongsToTeam && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não pertence a esse time");
        }

        for (User user : users) {
            user.setTeam(null);
        }

        for (Position position : team.getPositions()) {
            for (User user : position.getUsers()) {
                user.getPositions().remove(position);
            }
            position.getUsers().clear();
        }

        userRepository.saveAll(users);

        positionRepository.deleteByTeamId(team.getId());

        itemRepository.deleteByTeam(team);

        sprintRepository.deleteByTeam(team);

        teamInviteRepository.deleteByTeam(team);

        teamRepository.delete(team);


        return ResponseEntity.noContent().build();
    }

    @Transactional
    @GetMapping("/{teamId}/users")
    public ResponseEntity<List<User>> members(
            @PathVariable Long teamId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        User userRequest = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Seu usuário não foi encontrado"
                ));

        if(!userRequest.getTeam().getId().equals(teamId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você não pertence a esse time"
            );
        }

        var team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Time não encontrado"
                ));

        return ResponseEntity.ok(team.getUsers());
    }

    @Transactional
    @PostMapping("/{teamId}/invites")
    public ResponseEntity<String> createInvite(
            @PathVariable Long teamId,
            @AuthenticationPrincipal Jwt jwt
    ) {
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

        TeamInvite invite = new TeamInvite();
        invite.setToken(UUID.randomUUID().toString());
        invite.setTeam(team);
        invite.setExpiresAt(LocalDateTime.now().plusDays(7));
        invite.setMaxUses(20);
        invite.setUsedCount(0);
        invite.setActive(true);

        teamInviteRepository.save(invite);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(invite.getToken());
    }

    @Transactional
    @PostMapping("/invites/{token}/accept")
    public ResponseEntity<Void> acceptInvite(
            @PathVariable String token,
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID userId = UUID.fromString(jwt.getSubject());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuário não encontrado"
                ));


        if (user.getTeam() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Usuário já pertence a um time"
            );
        }

        TeamInvite invite = teamInviteRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Convite inválido"
                ));

        if (!invite.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Convite desativado"
            );
        }

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Convite expirado"
            );
        }

        if (invite.getUsedCount() >= invite.getMaxUses()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Convite já utilizado"
            );
        }

        user.setTeam(invite.getTeam());
        userRepository.save(user);

        invite.setUsedCount(invite.getUsedCount() + 1);

        if (invite.getUsedCount() >= invite.getMaxUses()) {
            invite.setActive(false);
        }

        teamInviteRepository.save(invite);

        return ResponseEntity.noContent().build();
    }

    @Transactional
    @PreAuthorize("hasRole('admin') or hasRole('scrummaster')")
    @DeleteMapping("/{teamId}/users/{memberId}")
    public ResponseEntity<Void> kickOutMember(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long teamId,
            @PathVariable UUID memberId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuário não encontrado"
                ));

        if (user.getTeam() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Usuário não está em nenhum time"
            );
        }

        if (!user.getTeam().getId().equals(teamId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você não pode remover membros de outro time"
            );
        }

        User member = userRepository.findById(memberId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Membro não encontrado"
        ));

        if(!user.getTeam().getUsers().contains(member)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Esse usuário não pertence ao seu time"
            );
        }

        member.setTeam(null);
        userRepository.save(member);
        return ResponseEntity.noContent().build();
    }

    @Transactional
    @PostMapping("/leave")
    public ResponseEntity<Void> leaveTeam(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuário não encontrado"
                ));

        if (user.getTeam() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Usuário não está em nenhum time"
            );
        }

        if (user.getTeam().getUsers().size() <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "O ultimo usuário deve deletar o time"
            );
        }

        user.setPositions(null);
        user.setTeam(null);

        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

}
