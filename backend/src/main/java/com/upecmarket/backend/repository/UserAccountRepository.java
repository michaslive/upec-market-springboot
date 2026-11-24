package com.upecmarket.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.upecmarket.backend.entity.UserAccountEntity;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long> {
}
