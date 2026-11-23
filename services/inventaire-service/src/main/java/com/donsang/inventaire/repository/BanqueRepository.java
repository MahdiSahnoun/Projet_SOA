package com.donsang.inventaire.repository;

import com.donsang.inventaire.domain.Banque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BanqueRepository extends JpaRepository<Banque, String> {}
