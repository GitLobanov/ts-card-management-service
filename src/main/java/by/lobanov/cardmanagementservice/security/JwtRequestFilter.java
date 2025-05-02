package by.lobanov.cardmanagementservice.security;

import by.lobanov.cardmanagementservice.service.impl.UserDetailsServiceImpl;
import by.lobanov.cardmanagementservice.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = parseJwt(request); // Извлекаем токен из заголовка

            if (jwt != null && jwtUtil.validateToken(jwt)) { // Проверяем наличие и валидность токена
                String username = jwtUtil.extractUsername(jwt); // Извлекаем email

                // Загружаем UserDetails
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Создаем объект аутентификации
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()); // Пароль не нужен, т.к. токен уже подтверждает аутентификацию

                // Добавляем детали аутентификации (IP, сессия и т.д.)
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Устанавливаем аутентификацию в SecurityContext
                // После этого пользователь считается аутентифицированным для данного запроса
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("User '{}' authenticated successfully via JWT.", username); // Используем debug уровень
            } else {
                if (jwt != null) {
                    logger.trace("JWT Token validation failed for token: {}", jwt); // Trace, т.к. невалидный токен не ошибка приложения
                } else {
                    logger.trace("No JWT token found in request to {}", request.getRequestURI());
                }
            }
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token is expired: {}", e.getMessage());
            // Можно установить специфичный атрибут запроса или заголовок ответа, если нужно
            // request.setAttribute("expired", e.getMessage());
            SecurityContextHolder.clearContext(); // Очищаем контекст, если токен истек
        } catch (JwtException | UsernameNotFoundException e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
            SecurityContextHolder.clearContext(); // Очищаем контекст при любой ошибке
        } catch (Exception e) {
            logger.error("An unexpected error occurred during JWT processing: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }


        filterChain.doFilter(request, response); // Передаем запрос дальше по цепочке фильтров
    }

    // Вспомогательный метод для извлечения токена из заголовка Authorization
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith(BEARER_PREFIX)) {
            return headerAuth.substring(BEARER_PREFIX.length()); // Обрезаем "Bearer "
        }

        return null; // Возвращаем null, если заголовок отсутствует или не соответствует формату
    }
}
