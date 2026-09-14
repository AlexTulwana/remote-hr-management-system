package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmployeeEmployeeNumber(String employeeNumber);
    Optional<User> findByEmployeeIdNumber(String idNumber);
    List<User> findByEmployeeFullNameContainingIgnoreCase(String query);
}
