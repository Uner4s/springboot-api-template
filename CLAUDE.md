# CLAUDE.md — Spring Boot

## Stack

| Technology                  | Purpose                       | Version     |
| --------------------------- | ----------------------------- | ----------- |
| Spring Boot                 | Backend framework             | 3.x         |
| Java                        | Language                      | 21 (LTS)    |
| Spring Data JPA + Hibernate | SQL ORM                       | see pom.xml |
| Spring Data MongoDB         | MongoDB ODM                   | see pom.xml |
| Spring Security             | Auth and authorization        | see pom.xml |
| JJWT (jjwt-api)             | JWT generation and validation | see pom.xml |
| MapStruct                   | Entity ↔ DTO mapping          | see pom.xml |
| Lombok                      | Boilerplate reduction         | see pom.xml |
| Bean Validation (Jakarta)   | DTO validation                | see pom.xml |
| dotenv-java / @Value        | Environment variable loading  | see pom.xml |

**Database options — use the one that matches the project:**

- `PostgreSQL + Spring Data JPA` — relational, entities defined with `@Entity`,
  migrations via Flyway
- `MongoDB + Spring Data MongoDB` — document database, documents defined with
  `@Document`
- Both can coexist in the same project — configure separate datasource beans

## Commands

```bash
./mvnw spring-boot:run                        # Dev server
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev  # With specific profile
./mvnw clean package                          # Build JAR
./mvnw test                                   # Run all tests
./mvnw test -Dtest=UserServiceTest            # Run specific test class
./mvnw flyway:migrate                         # Apply pending SQL migrations (Flyway)
./mvnw flyway:info                            # Show migration status
```

## Folder structure

```
src/
├── main/
│   ├── java/com/company/project/
│   │   ├── ProjectApplication.java     Entry point — @SpringBootApplication only, no logic
│   │   ├── config/                     Spring beans and global configuration
│   │   │   ├── SecurityConfig.java     Spring Security filter chain and rules
│   │   │   ├── JpaConfig.java          DataSource and JPA config (SQL projects)
│   │   │   ├── MongoConfig.java        MongoClient config (Mongo projects)
│   │   │   └── CorsConfig.java         CORS rules
│   │   ├── controllers/                HTTP endpoints — routing and I/O only, no logic
│   │   ├── services/                   Business logic — one class per domain
│   │   ├── repositories/               Data access interfaces — JPA or Mongo repositories
│   │   ├── entities/                   JPA entities (@Entity) — SQL only
│   │   ├── documents/                  Mongo documents (@Document) — Mongo only
│   │   ├── dtos/
│   │   │   ├── request/                Input DTOs — naming: ActionEntityRequest
│   │   │   └── response/               Output DTOs — naming: ActionEntityResponse
│   │   ├── mappers/                    MapStruct mappers — entity/document ↔ DTO
│   │   ├── security/                   JWT filter, UserDetails, permission evaluator
│   │   │   ├── JwtFilter.java
│   │   │   ├── JwtService.java
│   │   │   ├── UserDetailsServiceImpl.java
│   │   │   └── permissions/            Permission enums and evaluator
│   │   ├── exceptions/                 Custom exceptions and global handler
│   │   │   ├── ApiException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   └── helpers/                    Pure static utilities — no Spring beans when avoidable
│   └── resources/
│       ├── application.yml             Main config — all env vars loaded here
│       ├── application-dev.yml         Dev overrides
│       └── db/migration/               Flyway SQL migrations (V1__init.sql, V2__add_users.sql)
└── test/
    └── java/com/company/project/
        ├── controllers/                Integration tests — @SpringBootTest or @WebMvcTest
        └── services/                   Unit tests — plain JUnit + Mockito
```

**Rules:**

- One class per responsibility. No God classes.
- Global utilities go in `helpers/`. Security logic goes in `security/`. Never
  mix.
- `config/` contains only Spring `@Configuration` beans — no business logic.
- Never put `@Transactional` in controllers — only in services.

## Code rules (strictly enforced)

### Naming conventions

| Element             | Convention                       | Example                                      |
| ------------------- | -------------------------------- | -------------------------------------------- |
| Files / Classes     | `PascalCase`                     | `UserService.java`, `CreateUserRequest.java` |
| Methods / Variables | `camelCase`                      | `findByEmail`, `userId`                      |
| Constants           | `UPPER_SNAKE_CASE`               | `MAX_TOKEN_EXPIRY`                           |
| Packages            | `lowercase`                      | `controllers`, `dtos.request`                |
| Request DTOs        | `Action` + `Entity` + `Request`  | `CreateUserRequest`                          |
| Response DTOs       | `Action` + `Entity` + `Response` | `GetUserResponse`                            |
| Entities            | `PascalCase` (no suffix)         | `User`, `AuditLog`                           |
| Mongo Documents     | `PascalCase` (no suffix)         | `Event`, `Notification`                      |
| Repositories        | `Entity` + `Repository`          | `UserRepository`                             |
| Services            | `Entity` + `Service`             | `UserService`                                |
| Controllers         | `Entity` + `Controller`          | `UserController`                             |
| Mappers             | `Entity` + `Mapper`              | `UserMapper`                                 |
| Permission enums    | `PascalCase`                     | `AppPermission.MANAGE_USERS`                 |

### Controllers

- Annotated with `@RestController` and `@RequestMapping("/api/v1/entity")`
- Constructor injection only — never `@Autowired` on fields
- Always return `ResponseEntity<ApiResponse<T>>`
- Delegate all logic to services — controllers are routing + I/O only
- Validate input with `@Valid` on `@RequestBody` and `@RequestParam`
- Protect routes with
  `@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'PERMISSION_NAME')")`

```java
// controllers/UserController.java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'READ_USERS')")
    public ResponseEntity<ApiResponse<List<GetUserResponse>>> getAll() {
        List<GetUserResponse> data = userService.findAll();
        return ResponseEntity.ok(ApiResponse.success(data, "Users retrieved"));
    }

    @PostMapping
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'MANAGE_USERS')")
    public ResponseEntity<ApiResponse<GetUserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        GetUserResponse data = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data, "User created"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'MANAGE_USERS')")
    public ResponseEntity<ApiResponse<GetUserResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request) {
        GetUserResponse data = userService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(data, "User updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'MANAGE_USERS')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted"));
    }
}
```

### Services

- Annotated with `@Service`
- Constructor injection only
- All business logic, queries, validations, and side effects live here
- `@Transactional` on methods that write to the database (SQL only)
- Throw `ApiException` for controlled errors — never return null to signal
  failure
- Use `MapStruct` mappers for all entity ↔ DTO conversions — never map manually
  in services

```java
// services/UserService.java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public List<GetUserResponse> findAll() {
        return userRepository.findAllByIsDeletedFalse()
                .stream()
                .map(userMapper::toGetResponse)
                .toList();
    }

    @Transactional
    public GetUserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("Email already registered", HttpStatus.BAD_REQUEST);
        }
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User saved = userRepository.save(user);
        return userMapper.toGetResponse(saved);
    }

    @Transactional
    public void delete(String id) {
        User user = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        user.setIsDeleted(true);
        userRepository.save(user);
    }
}
```

### Repositories

- Interfaces that extend `JpaRepository<Entity, ID>` (SQL) or
  `MongoRepository<Document, ID>` (Mongo)
- No implementation class — Spring Data generates queries from method names
- Complex queries use `@Query` with JPQL (JPA) or JSON query (Mongo) — never
  native SQL unless justified
- Read-only queries should use projections or DTOs via `@Query` + `new`
  constructor expression

```java
// repositories/UserRepository.java
public interface UserRepository extends JpaRepository<User, String> {

    List<User> findAllByIsDeletedFalse();

    Optional<User> findByIdAndIsDeletedFalse(String id);

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.role.name = :roleName AND u.isDeleted = false")
    List<User> findAllByRoleName(@Param("roleName") String roleName);
}
```

### Entities (SQL — JPA)

- Annotated with `@Entity` and `@Table(name = "table_name")`
- Extend `BaseEntity` (id, createdAt, updatedAt, isDeleted)
- IDs as UUID strings — generated via `@UuidGenerator`
- Enums stored as strings with `@Enumerated(EnumType.STRING)` — never as
  integers
- Relationships always declared with explicit `fetch = FetchType.LAZY` — never
  EAGER
- Never use `CascadeType.ALL` — be explicit about cascade behavior

```java
// entities/BaseEntity.java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private String id;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean isDeleted = false;
}
```

```java
// entities/User.java
@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;
}
```

### Documents (Mongo — Spring Data MongoDB)

- Annotated with `@Document(collection = "collection_name")`
- Extend `BaseDocument` (id, createdAt, updatedAt)
- IDs as String — MongoDB generates ObjectId automatically
- Enums stored as strings
- Embedded objects use `@Field` — never nested `@Document` references unless
  necessary

```java
// documents/BaseDocument.java
@Getter @Setter
public abstract class BaseDocument {

    @Id
    private String id;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

```java
// documents/Event.java
@Document(collection = "events")
@Getter @Setter
@NoArgsConstructor
public class Event extends BaseDocument {

    @Field("type")
    private String type;

    @Field("payload")
    private Map<String, Object> payload;

    @Field("user_id")
    private String userId;
}
```

### DTOs

- Request DTOs validated with Jakarta Bean Validation annotations
- Validation messages in Spanish — they are end-user facing
- Response DTOs are plain records or classes — no validation annotations
- Use `@NotBlank` over `@NotNull` for strings — blank strings pass `@NotNull`
- MapStruct handles all mapping — never map field by field in services or
  controllers

```java
// dtos/request/CreateUserRequest.java
@Getter @Setter
public class CreateUserRequest {

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String firstName;

    @NotBlank(message = "El apellido es requerido")
    private String lastName;

    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe tener un formato válido")
    private String email;

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "El rol es requerido")
    private String roleId;
}
```

```java
// dtos/response/GetUserResponse.java
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class GetUserResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String roleName;
    private LocalDateTime createdAt;
}
```

### Mappers (MapStruct)

- Interfaces annotated with `@Mapper(componentModel = "spring")`
- One mapper per entity/document
- Use `@Mapping` for field name differences or computed fields
- Never instantiate mappers manually — inject via constructor

```java
// mappers/UserMapper.java
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "role.name", target = "roleName")
    GetUserResponse toGetResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "password", ignore = true)  // set manually in service after encoding
    @Mapping(target = "role", ignore = true)       // resolved in service by roleId
    User toEntity(CreateUserRequest request);
}
```

### Standardized responses

All endpoints return the same response shape. Never construct response payloads
inline in controllers.

```java
// dtos/response/ApiResponse.java
@Getter @AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String message;
    private String error;

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    public static <T> ApiResponse<T> error(String error) {
        return new ApiResponse<>(false, null, null, error);
    }
}
```

### Exception handling

- `ApiException` for all controlled errors — wraps message + HTTP status
- `GlobalExceptionHandler` with `@RestControllerAdvice` catches everything — no
  try/catch in controllers
- Validation errors (`@Valid`) are caught automatically and formatted
  consistently

```java
// exceptions/ApiException.java
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
```

```java
// exceptions/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Validation error");
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error"));
    }
}
```

## Security — JWT + granular permissions

### Permission enum

Define all feature-level permissions as an enum. A role holds a set of these
permissions.

```java
// security/permissions/AppPermission.java
public enum AppPermission {
    // Users
    READ_USERS,
    MANAGE_USERS,
    // Audits
    READ_AUDITS,
    MANAGE_AUDITS,
    // Reports
    READ_REPORTS,
    EXPORT_REPORTS,
    // Admin
    SUPER_ADMIN  // bypasses all checks
}
```

### Role entity with permissions

```java
// entities/Role.java
@Entity
@Table(name = "roles")
@Getter @Setter @NoArgsConstructor
public class Role extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    private Set<AppPermission> permissions = new HashSet<>();
}
```

### JWT service

```java
// security/JwtService.java
@Service
public class JwtService {

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.expiration-ms}")
    private long expirationMs;

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().getName());
        claims.put("permissions", user.getRole().getPermissions()
                .stream().map(Enum::name).toList());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException e) {
            return false;
        }
    }
}
```

### JWT filter

```java
// security/JwtFilter.java
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            chain.doFilter(request, response);
            return;
        }

        String email = jwtService.extractClaims(token).getSubject();
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(request, response);
    }
}
```

### Permission evaluator (replaces CASL)

```java
// security/permissions/AppPermissionEvaluator.java
@Component("permissionEvaluator")
public class AppPermissionEvaluator {

    public boolean hasPermission(Authentication authentication, String permissionName) {
        if (authentication == null || !authentication.isAuthenticated()) return false;

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth ->
                        auth.equals("PERMISSION_" + permissionName) ||
                        auth.equals("PERMISSION_SUPER_ADMIN")
                );
    }
}
```

### SecurityConfig

```java
// config/SecurityConfig.java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // enables @PreAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

### Auth controller and flow

```java
// controllers/AuthController.java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse data = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(data, "Login successful"));
    }
}
```

```java
// services/AuthService.java
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .orElseThrow(() -> new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtService.generateToken(user);
        return new LoginResponse(token, user.getEmail(), user.getRole().getName());
    }
}
```

## Database configuration

### SQL — PostgreSQL + JPA (application.yml)

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate # never create or update in production — use Flyway
    show-sql: false # true only in dev profile
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

security:
  jwt:
    secret: ${JWT_SECRET}
    expiration-ms: ${JWT_EXPIRATION_MS:86400000} # default 24h
```

### Mongo — Spring Data MongoDB (application.yml)

```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI}
      database: ${MONGODB_DATABASE}
```

### Both databases in the same project

```java
// config/JpaConfig.java — SQL datasource
@Configuration
@EnableJpaRepositories(
    basePackages = "com.company.project.repositories.sql",
    entityManagerFactoryRef = "sqlEntityManagerFactory",
    transactionManagerRef = "sqlTransactionManager"
)
public class JpaConfig { ... }

// config/MongoConfig.java — Mongo datasource
@Configuration
@EnableMongoRepositories(
    basePackages = "com.company.project.repositories.mongo"
)
public class MongoConfig { ... }
```

> When using both databases, SQL repositories go in `repositories/sql/` and
> Mongo repositories in `repositories/mongo/` to avoid Spring's auto-detection
> conflicts.

## Tests

- Unit tests use `@ExtendWith(MockitoExtension.class)` — no Spring context, fast
- Integration tests use `@SpringBootTest` + `@AutoConfigureMockMvc` — full
  context
- Controller tests can use `@WebMvcTest(UserController.class)` for lighter
  context
- Mock repositories and external services with `@MockBean` in integration tests
- Use `@Sql` or `@BeforeEach` to seed test data — never rely on production data
- Test file naming: `ClassNameTest.java`

```java
// services/UserServiceTest.java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void create_whenEmailAlreadyExists_throwsApiException() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("test@example.com");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(ApiException.class, () -> userService.create(request));
        verify(userRepository, never()).save(any());
    }
}
```

## What NOT to do

- **No `@Autowired` on fields** — always constructor injection. Field injection
  hides dependencies and breaks testability.
- **No business logic in controllers** — controllers do routing + I/O only.
  Logic belongs in services.
- **No direct repository access in controllers** — always through services.
- **No manual field-by-field mapping** — that's what MapStruct is for. Manual
  mapping breaks silently when fields are added.
- **No `ddl-auto: create` or `update` outside local throwaway experiments** —
  always use Flyway migrations in any persistent environment.
- **No `FetchType.EAGER`** on relationships — it generates N+1 queries and loads
  data you didn't ask for.
- **No `CascadeType.ALL`** — be explicit. `PERSIST` and `MERGE` are usually
  enough.
- **No hardcoded secrets** — no passwords, JWT secrets, or connection strings in
  `application.yml`. Always use `${ENV_VAR}`.
- **No `@Transactional` in controllers** — only in services.
- **No `try/catch` in controllers** — `GlobalExceptionHandler` handles
  everything. Controllers should be clean.
- **No `Optional.get()` without `isPresent()` check** — always use
  `.orElseThrow()` with a meaningful `ApiException`.
- **No `System.out.println`** — use SLF4J
  (`private static final Logger log = LoggerFactory.getLogger(...)` or Lombok's
  `@Slf4j`).
- **No nullable `Boolean` fields** — use primitive `boolean` or default to
  `false` on entities.
- **No `@Query` with native SQL** unless JPQL is insufficient and the reason is
  documented.

## Architecture decisions

**Strict layered architecture (Controllers → Services → Repositories):** Each
layer has one responsibility. Controllers never access repositories.
Repositories have no business logic. This mirrors the pattern in the NestJS and
.NET configs and keeps each layer independently testable.

**MapStruct over manual mapping:** MapStruct generates compile-time mapping code
— it fails fast if a field is missing, generates zero reflection overhead at
runtime, and keeps services clean. Manual mapping in services is banned.

**Flyway for schema management:** `ddl-auto: validate` in all persistent
environments. Flyway migrations live in `db/migration/` as versioned SQL files
(`V1__init.sql`). This makes schema changes auditable, reversible, and safe in
production.

**Granular permissions via `AppPermission` enum + `@PreAuthorize`:**
Authorization is not role-based but permission-based. A role holds a set of
`AppPermission` values. The JWT embeds those permissions as claims.
`AppPermissionEvaluator` checks them on every protected endpoint via
`@PreAuthorize`. `SUPER_ADMIN` bypasses all checks. This mirrors the
`ERolePermission` pattern in the .NET config.

**JWT embeds permissions at login time:** The token carries the user's full
permission set. No database lookup is needed per request — the filter validates
the token and loads authorities from its claims. This keeps the filter stateless
and fast.

**`ApiResponse<T>` as the universal wrapper:** Every endpoint returns the same
shape (`success`, `data`, `message`, `error`). `GlobalExceptionHandler` formats
errors into the same shape. Frontend handling is predictable across the entire
API.

**`ApiException` as the single controlled error mechanism:** Services throw
`ApiException(message, HttpStatus)`. `GlobalExceptionHandler` catches it and
maps it to the right HTTP response. No `try/catch` blocks in controllers. No
status codes scattered across the codebase.

**Constructor injection everywhere:** `@RequiredArgsConstructor` (Lombok)
generates the constructor from `final` fields. This makes dependencies explicit,
supports immutability, and makes unit testing with Mockito straightforward.

**Dual-database setup (SQL + Mongo) with explicit package separation:** When
both databases are needed, JPA repositories live in `repositories/sql/` and
Mongo repositories in `repositories/mongo/`. Separate `@Configuration` classes
enable each one explicitly. This avoids Spring's auto-detection conflicts and
makes the boundary between stores visible.
