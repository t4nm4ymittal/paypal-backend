package com.paypal.user_service.Controller;

import com.paypal.user_service.Dto.LoginReq;
import com.paypal.user_service.Dto.SignupReq;
import com.paypal.user_service.Dto.JwtReq;
import com.paypal.user_service.Dto.UserResp;
import com.paypal.user_service.Service.UserService;
import com.paypal.user_service.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.paypal.user_service.Util.JWTUtil;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;

    public AuthController(UserService userService, PasswordEncoder passwordEncoder, JWTUtil jwtUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupReq req) throws URISyntaxException {
        try {
            User newUser = new User(req.getUsername(), req.getEmail(), passwordEncoder.encode(req.getPassword()),"ROLE_USER");
            User created = userService.createUser(newUser);
            UserResp resp = new UserResp(created.getId(), created.getUsername(), created.getEmail(), created.getCreatedAt());
            URI location = new URI("/api/users/" + created.getId());
            return ResponseEntity.created(location).body(resp);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginReq req) {
        Optional<User> opt = userService.findByEmail(req.getEmail());
        if (opt.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        User user = opt.get();
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getId(),user.getEmail(),user.getRole());

        return ResponseEntity.ok(new JwtReq(token));
    }
}
