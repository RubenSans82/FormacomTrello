package com.example.formacomtrello.service.impl;

import com.example.formacomtrello.model.User;
import com.example.formacomtrello.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
        grantedAuthorities.add(new SimpleGrantedAuthority(user.getRole().name()));

        // Manejo especial si la contraseña es null (colaborador primera vez)
        // Spring Security espera una contraseña no nula. Puedes poner un placeholder
        // o manejar esto de forma diferente en tu lógica de login/set-password.
        // Aquí asumimos que el flujo de 'set-password' se maneja ANTES del login normal.
        // Si intentan hacer login normal sin password, fallará aquí o en la comparación.
        String password = user.getPassword() != null ? user.getPassword() : "PLACEHOLDER_PASSWORD_NEEDS_SETTING";


        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                password, // Ojo: Si es null, esto dará error. Manejar el flujo de primera vez.
                grantedAuthorities
        );
    }
}