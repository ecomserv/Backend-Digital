package yea.ecomservapi.modules.clients.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yea.ecomservapi.modules.clients.domain.Client;
import yea.ecomservapi.modules.clients.dto.ClientDTO;
import yea.ecomservapi.modules.clients.repository.ClientRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;

    public List<ClientDTO> searchClients(String search) {
        List<Client> clients;
        if (search == null || search.isBlank()) {
            clients = clientRepository.findTop10ByOrderByUpdatedAtDesc();
        } else {
            String term = search.trim();
            clients = clientRepository.findByNameContainingIgnoreCaseOrRucContaining(term, term);
        }
        return clients.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<ClientDTO> findById(Long id) {
        return clientRepository.findById(id).map(this::toDTO);
    }

    public Optional<ClientDTO> findByRuc(String ruc) {
        return clientRepository.findByRuc(ruc).map(this::toDTO);
    }

    public ClientDTO createClient(ClientDTO dto) {
        Client client = toEntity(dto);
        Client saved = clientRepository.save(client);
        return toDTO(saved);
    }

    public Optional<ClientDTO> updateClient(Long id, ClientDTO dto) {
        return clientRepository.findById(id)
                .map(existing -> {
                    existing.setName(dto.getName());
                    existing.setRuc(dto.getRuc());
                    existing.setAddress(dto.getAddress());
                    existing.setPhone(dto.getPhone());
                    existing.setEmail(dto.getEmail());
                    existing.setContactPerson(dto.getContactPerson());
                    return toDTO(clientRepository.save(existing));
                });
    }

    public void deleteClient(Long id) {
        clientRepository.deleteById(id);
    }

    private ClientDTO toDTO(Client client) {
        return ClientDTO.builder()
                .id(client.getId())
                .name(client.getName())
                .ruc(client.getRuc())
                .address(client.getAddress())
                .phone(client.getPhone())
                .email(client.getEmail())
                .contactPerson(client.getContactPerson())
                .build();
    }

    private Client toEntity(ClientDTO dto) {
        return Client.builder()
                .name(dto.getName())
                .ruc(dto.getRuc())
                .address(dto.getAddress())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .contactPerson(dto.getContactPerson())
                .build();
    }
}
