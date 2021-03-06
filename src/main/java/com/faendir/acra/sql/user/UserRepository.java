package com.faendir.acra.sql.user;

import com.faendir.acra.sql.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

/**
 * @author Lukas
 * @since 20.05.2017
 */
public interface UserRepository extends JpaRepository<User, String> {
    Slice<User> findAllByRoles(@NonNull String role, @NonNull Pageable pageable);

    int countAllByRoles(@NonNull String role);
}
