package com.pipeline.crm.user;

import com.pipeline.crm.security.CurrentUser;
import com.pipeline.crm.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public List<UserDto> list() {
        return userService.list();
    }

    @GetMapping("/{id}")
    public UserDto get(@PathVariable UUID id) {
        return userService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request, actor());
    }

    @PatchMapping("/{id}")
    public UserDto update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request, actor());
    }

    @PostMapping("/{id}/block")
    public UserDto block(@PathVariable UUID id) {
        return userService.block(id, actor());
    }

    @PostMapping("/{id}/unblock")
    public UserDto unblock(@PathVariable UUID id) {
        return userService.unblock(id, actor());
    }

    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request, actor());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        userService.delete(id, actor());
    }

    private CurrentUser actor() {
        return securityUtils.currentUser();
    }
}
