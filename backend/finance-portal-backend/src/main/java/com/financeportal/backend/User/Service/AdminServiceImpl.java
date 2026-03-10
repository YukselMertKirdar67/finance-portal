package com.financeportal.backend.User.Service;


import com.financeportal.backend.Exception.ResourceNotFoundException;
import com.financeportal.backend.User.Entity.User;
import com.financeportal.backend.User.UserMapper;
import com.financeportal.backend.User.Repository.UserRepository;
import com.financeportal.backend.User.DTO.UserResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toUserResponseDTO)
                .toList();
    }

    @Override
    public void disableUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with id: " + userId));

        user.setEnabled(false);
        userRepository.save(user);
    }
}
