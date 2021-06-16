package com.eudes.dscatalog.services;

import java.util.Optional;

import javax.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eudes.dscatalog.dto.RoleDTO;
import com.eudes.dscatalog.dto.UserDTO;
import com.eudes.dscatalog.dto.UserInsertDTO;
import com.eudes.dscatalog.dto.UserUpdateDTO;
import com.eudes.dscatalog.entities.Role;
import com.eudes.dscatalog.entities.User;
import com.eudes.dscatalog.repositories.RoleRepository;
import com.eudes.dscatalog.repositories.UserRepository;
import com.eudes.dscatalog.services.exceptions.DatabaseException;
import com.eudes.dscatalog.services.exceptions.ResourceNotFoundException;

@Service // Annotation que registra a classe como parte do sistema de injeção de dependências do sistema
public class UserService {
	
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	@Autowired
	private UserRepository repository;
	
	@Autowired
	private RoleRepository roleRepository;
	
	@Transactional(readOnly = true)	
	public Page<UserDTO> findAllPaged(Pageable pageable) {
		
		Page<User> list = repository.findAll(pageable);
		
		// Como o PAGE já é um STREAM do JAVA 8, não precisa do método STREAM e nem do COLLECT
		return list.map(x -> new UserDTO(x)); 
	}

	@Transactional(readOnly = true)
	public UserDTO findById(Long id) {
		
		Optional<User> obj = repository.findById(id);
		User entity = obj.orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
		
		return new UserDTO(entity);
	}

	@Transactional
	public UserDTO insert(UserInsertDTO dto) {
		
		User entity = new User();
		copyDtoToEntity(dto, entity);
		entity.setPassword(passwordEncoder.encode(dto.getPassword())); // Criptografando a senha (Ver BEAN e BCryptPasswordEncoder)
		entity = repository.save(entity);
		
		return new UserDTO(entity);
	}

	@Transactional
	public UserDTO update(Long id, UserUpdateDTO dto) {
		
		try {
			User entity = repository.getOne(id); // GETONE ainda não está 'mexendo' no BD
			copyDtoToEntity(dto, entity);		
			entity = repository.save(entity);
			
			return new UserDTO(entity);
		}
		catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Id not found: " + id);
		}
	}
	
	public void delete(Long id) {
		
		try {
			repository.deleteById(id);
		}
		catch (EmptyResultDataAccessException e) {
			throw new ResourceNotFoundException("Id not found: " + id);
		}
		catch (DataIntegrityViolationException e) {
			throw new DatabaseException("Integrity violation");
		}
		
	}
	
	// Método PRIVADO que auxiliará no INSERT e no UPDATE do UserDTO
	private void copyDtoToEntity(UserDTO dto, User entity) {
		
		entity.setFirstName(dto.getFirstName());
		entity.setLastName(dto.getLastName());
		entity.setEmail(dto.getEmail());
				
		entity.getRoles().clear();
		for (RoleDTO roleDto : dto.getRoles()) {
			// O GETONE serve para capturar o ID de um DTO sem chegar no BD ainda. 
			// Ele só instancia uma entidade monitorada pelo JPA
			Role role = roleRepository.getOne(roleDto.getId());
			entity.getRoles().add(role);
		}
	}
 }
