package com.lap.service;

import com.lap.dto.AuthDTO;
import com.lap.entity.User;
import com.lap.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    public AuthDTO.UserResponse getCurrentUser(String username) {
        User user = userRepository
            .findByUsername(username)
            .orElseThrow(() ->
                new UsernameNotFoundException(
                    "User not found with username: " + username
                )
            );

        return new AuthDTO.UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getAvatarUrl()
        );
    }
}
