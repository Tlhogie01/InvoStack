package com.tman.invostack.service;

import com.tman.invostack.entities.Client;
import com.tman.invostack.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    // --- CRUD Operations ---

    public Client createClient(Client client) {
        return clientRepository.save(client);
    }

    public Client updateClient(Long id, Client updatedClient) {
        Client existing = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + id));
        existing.setName(updatedClient.getName());
        existing.setEmail(updatedClient.getEmail());
        existing.setAddress(updatedClient.getAddress());
        return clientRepository.save(existing);
    }

    public Client findById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + id));
    }

    public List<Client> findAllClients() {
        return clientRepository.findAll();
    }

    public Client findByEmail(String email) {
        return clientRepository.findByEmail(email);
    }

    public List<Client> searchClientsByName(String namePart) {
        return clientRepository.findByNameContainingIgnoreCase(namePart);
    }

    public void deleteClient(Long id) {
        Client client = findById(id);
        // Prevent deleting a client that has existing invoices (optional business rule)
        if (!client.getInvoices().isEmpty()) {
            throw new RuntimeException("Cannot delete client with existing invoices.");
        }
        clientRepository.delete(client);
    }
}