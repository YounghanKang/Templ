# Hackathon Monorepo & Spring Boot Guidelines

## 1. Project Context
- This is a monorepo structure containing `/front` (Frontend) and `/backend` (Spring Boot).
- Time limit is extremely short (6 days). Priority is given to speed and basic integration over perfect architecture.

## 2. Frontend-First Principle
- Before generating any DTO, Entity, or Controller in Spring Boot, ALWAYS check data types, request/response structures defined in `/front/src`.
- Match field names with Frontend JSON properties exact casing (e.g., camelCase).

## 3. Backend Implementation Rules
- **No Spring Security:** Do NOT implement Spring Security or complex OAuth filters. Use simple HTTP headers or mock interceptors if authentication is required.
- **Simplify JPA:** Do NOT use complex JPA entity mappings (`@OneToMany`, `@ManyToMany`, bidirectional relationships) to avoid recursion and lazy-loading issues. Keep entities flat.
- **DTO Usage:** Always use dedicated DTOs for Controller requests and responses instead of returning raw JPA Entities.
- **Global CORS:** Ensure CORS is enabled for frontend local server origins.

## 4. Code Style & Tooling
- Use **Java 17** features and **Lombok** annotations (`@Getter`, `@RequiredArgsConstructor`, `@Builder`, etc.) to minimize boilerplate code.