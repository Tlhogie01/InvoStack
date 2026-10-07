package com.tman.invostack.repository;

import com.tman.invostack.entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    // Finds a client by their exact email (useful for authentication)
    Client findByEmail(String email);

    // Finds all clients whose name contains the given text (case insensitive)
    List<Client> findByNameContainingIgnoreCase(String namePart);
}