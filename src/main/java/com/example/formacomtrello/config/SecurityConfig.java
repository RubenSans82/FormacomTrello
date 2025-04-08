package com.example.formacomtrello.config;

import com.example.formacomtrello.service.impl.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // Cambio aquí
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher; // Para logout

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Habilita @PreAuthorize, reemplaza EnableGlobalMethodSecurity
public class SecurityConfig { // YA NO extiende WebSecurityConfigurerAdapter

    // Inyecta tu UserDetailsService personalizado
    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Bean para configurar el AuthenticationManager (necesario para procesar UserDetailsService)
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Configuración de Autorización de Peticiones
                .authorizeHttpRequests(authorize -> authorize
                        // Recursos estáticos y páginas públicas (usamos requestMatchers)
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers("/", "/home", "/register", "/login", "/process-register", "/set-password", "/process-set-password").permitAll()
                        // Rutas específicas de Gestor
                        .requestMatchers("/projects/new", "/projects/*/edit", "/projects/*/invite", "/projects/*/tasks/new", "/tasks/*/edit", "/tasks/*/delete").hasAuthority("GESTOR")
                        // Rutas específicas de Colaborador
                        .requestMatchers("/collaborator/**", "/tasks/*/complete").hasAuthority("COLABORADOR")
                        // Rutas que requieren autenticación (ambos roles pueden ver detalles)
                        .requestMatchers("/manager/**", "/projects/**", "/tasks/**", "/user/profile", "/tasks/*/comment").authenticated()
                        // Cualquier otra petición requiere autenticación
                        .anyRequest().authenticated()
                )
                // Configuración del Formulario de Login
                .formLogin(form -> form
                        .loginPage("/login") // Página de login personalizada
                        .loginProcessingUrl("/perform_login") // URL que procesa el login
                        .defaultSuccessUrl("/home-redirect", true) // Redirigir a /home después del login exitoso
                        .failureUrl("/login?error=true") // Redirigir si falla el login
                        .permitAll() // Permitir acceso a la página de login y procesamiento a todos
                )
                // Configuración del Logout
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout")) // URL para logout
                        .logoutSuccessUrl("/login?logout=true") // Redirigir después del logout
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll() // Permitir acceso a la URL de logout a todos
                )
                // Configuración del Manejo de Excepciones (Opcional, para páginas de error personalizadas)
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedPage("/error/403") // Página para acceso denegado (Forbidden 403)
                );
        // Deshabilitar CSRF temporalmente para pruebas si usas Postman, ¡HABILITAR en producción!
        // .csrf(csrf -> csrf.disable());

        // Construir y devolver el SecurityFilterChain
        return http.build();
    }
}