---
name: spring-boot-test-generator
description: >
  Generates complete, production-grade test suites for Spring Boot MVC applications.
  Use this skill whenever the user asks to write tests, generate test cases, test a layer,
  add unit tests, integration tests, or verify Spring Boot code. Trigger on phrases like
  "write tests for my service", "test my controller", "generate unit tests", "add tests to",
  "test the repository", "mock the service", "write integration tests", "test my API endpoint",
  or any request to verify Spring Boot MVC behavior. Also trigger when the user shares a
  Controller, Service, Repository, or Entity and asks if it works or how to test it.
  Always use this skill for Spring Boot test generation — do not wing it from memory.
---

# Spring Boot MVC Test Generator

## Project Context

- **Language**: Java 17+
- **Framework**: Spring Boot (latest stable)
- **Build tool**: Maven
- **Test stack**: JUnit 5 + Mockito + MockMvc + Testcontainers
- **Architecture tested**: MVC — Controller, Service, Repository, Mapper layers
- **Assertion style**: AssertJ (`assertThat(...)`) — never use JUnit `assertEquals` directly
- **Mocking**: Mockito — `@Mock`, `@InjectMocks`, `@MockBean` where appropriate
- **DB tests**: `@DataJpaTest` + Testcontainers (MySQL) for repository layer

---

## Layer-to-Test Mapping

When the user provides code or names a layer, generate the corresponding test type:

| Layer | Test type | Annotation |
|---|---|---|
| Controller | MockMvc slice test | `@WebMvcTest` |
| Service | Pure unit test | `@ExtendWith(MockitoExtension.class)` |
| Repository | JPA slice test | `@DataJpaTest` + Testcontainers |
| Mapper | Pure unit test | `@ExtendWith(MockitoExtension.class)` |
| Full flow | Integration test | `@SpringBootTest` + `@AutoConfigureMockMvc` |

Always ask the user which layer to test if they haven't specified. Generate one layer at a time unless they explicitly ask for all layers.

---

## 1. Service Layer — Unit Tests

- Use `@ExtendWith(MockitoExtension.class)` — no Spring context, runs instantly
- Mock the repository and mapper with `@Mock`
- Inject mocks into the service impl with `@InjectMocks`
- Test: happy path, not-found exception, edge cases (null, empty list, duplicate)
- Use `verify(...)` to assert interactions, not just return values
- Use `assertThatThrownBy(...)` for exception assertions — never try/catch in tests

**Template:**
```java
@ExtendWith(MockitoExtension.class)
class ExampleServiceImplTest {

    @Mock
    private ExampleRepository repository;

    @Mock
    private ExampleMapper mapper;

    @InjectMocks
    private ExampleServiceImpl service;

    // --- CREATE ---

    @Test
    @DisplayName("create() - should save entity and return response")
    void create_shouldSaveAndReturnResponse() {
        ExampleRequest request = new ExampleRequest("test-name");
        ExampleEntity entity = ExampleEntity.builder().id(1L).name("test-name").build();
        ExampleResponse response = ExampleResponse.builder().id(1L).name("test-name").build();

        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);

        ExampleResponse result = service.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("test-name");
        verify(repository).save(entity);
    }

    // --- GET BY ID ---

    @Test
    @DisplayName("getById() - should return response when entity exists")
    void getById_shouldReturnResponse_whenEntityExists() {
        ExampleEntity entity = ExampleEntity.builder().id(1L).name("test-name").build();
        ExampleResponse response = ExampleResponse.builder().id(1L).name("test-name").build();

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        ExampleResponse result = service.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getById() - should throw ResourceNotFoundException when entity not found")
    void getById_shouldThrowException_whenEntityNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- GET ALL ---

    @Test
    @DisplayName("getAll() - should return list of responses")
    void getAll_shouldReturnListOfResponses() {
        List<ExampleEntity> entities = List.of(
                ExampleEntity.builder().id(1L).name("a").build(),
                ExampleEntity.builder().id(2L).name("b").build()
        );
        List<ExampleResponse> responses = List.of(
                ExampleResponse.builder().id(1L).name("a").build(),
                ExampleResponse.builder().id(2L).name("b").build()
        );

        when(repository.findAll()).thenReturn(entities);
        when(mapper.toResponseList(entities)).thenReturn(responses);

        List<ExampleResponse> result = service.getAll();

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getAll() - should return empty list when no entities exist")
    void getAll_shouldReturnEmptyList_whenNoEntities() {
        when(repository.findAll()).thenReturn(Collections.emptyList());
        when(mapper.toResponseList(Collections.emptyList())).thenReturn(Collections.emptyList());

        List<ExampleResponse> result = service.getAll();

        assertThat(result).isEmpty();
    }

    // --- UPDATE ---

    @Test
    @DisplayName("update() - should update and return updated response")
    void update_shouldUpdateEntity() {
        ExampleEntity existing = ExampleEntity.builder().id(1L).name("old").build();
        ExampleRequest request = new ExampleRequest("new-name");
        ExampleEntity updated = ExampleEntity.builder().id(1L).name("new-name").build();
        ExampleResponse response = ExampleResponse.builder().id(1L).name("new-name").build();

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(ExampleEntity.class))).thenReturn(updated);
        when(mapper.toResponse(updated)).thenReturn(response);

        ExampleResponse result = service.update(1L, request);

        assertThat(result.getName()).isEqualTo("new-name");
    }

    // --- DELETE ---

    @Test
    @DisplayName("delete() - should call deleteById when entity exists")
    void delete_shouldDeleteEntity_whenExists() {
        when(repository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("delete() - should throw ResourceNotFoundException when entity not found")
    void delete_shouldThrow_whenEntityNotFound() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

---

## 2. Controller Layer — MockMvc Tests

- Use `@WebMvcTest(ExampleController.class)` — loads only web layer, fast
- Mock the service with `@MockBean`
- Use `MockMvc` injected via `@Autowired`
- Test: 200/201/204 happy paths, 400 validation errors, 404 not found, 405 method not allowed
- Use `perform(...)` + `.andExpect(...)` chains
- Use `ObjectMapper` to serialize request bodies to JSON
- If endpoints are secured with JWT, add `@WithMockUser` or disable security in the test slice

**Template:**
```java
@WebMvcTest(ExampleController.class)
class ExampleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExampleService exampleService;

    @Autowired
    private ObjectMapper objectMapper;

    // --- POST ---

    @Test
    @DisplayName("POST /api/v1/examples - should return 201 with created resource")
    void create_shouldReturn201() throws Exception {
        ExampleRequest request = new ExampleRequest("test-name");
        ExampleResponse response = ExampleResponse.builder().id(1L).name("test-name").build();

        when(exampleService.create(any(ExampleRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/examples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("test-name"));
    }

    @Test
    @DisplayName("POST /api/v1/examples - should return 400 when name is blank")
    void create_shouldReturn400_whenNameIsBlank() throws Exception {
        ExampleRequest request = new ExampleRequest("");

        mockMvc.perform(post("/api/v1/examples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- GET BY ID ---

    @Test
    @DisplayName("GET /api/v1/examples/{id} - should return 200 with resource")
    void getById_shouldReturn200() throws Exception {
        ExampleResponse response = ExampleResponse.builder().id(1L).name("test-name").build();

        when(exampleService.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/examples/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("test-name"));
    }

    @Test
    @DisplayName("GET /api/v1/examples/{id} - should return 404 when not found")
    void getById_shouldReturn404_whenNotFound() throws Exception {
        when(exampleService.getById(99L))
                .thenThrow(new ResourceNotFoundException("Example not found with id: 99"));

        mockMvc.perform(get("/api/v1/examples/99"))
                .andExpect(status().isNotFound());
    }

    // --- GET ALL ---

    @Test
    @DisplayName("GET /api/v1/examples - should return 200 with list")
    void getAll_shouldReturn200WithList() throws Exception {
        List<ExampleResponse> responses = List.of(
                ExampleResponse.builder().id(1L).name("a").build(),
                ExampleResponse.builder().id(2L).name("b").build()
        );

        when(exampleService.getAll()).thenReturn(responses);

        mockMvc.perform(get("/api/v1/examples"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // --- PUT ---

    @Test
    @DisplayName("PUT /api/v1/examples/{id} - should return 200 with updated resource")
    void update_shouldReturn200() throws Exception {
        ExampleRequest request = new ExampleRequest("updated-name");
        ExampleResponse response = ExampleResponse.builder().id(1L).name("updated-name").build();

        when(exampleService.update(eq(1L), any(ExampleRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/examples/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("updated-name"));
    }

    // --- DELETE ---

    @Test
    @DisplayName("DELETE /api/v1/examples/{id} - should return 204")
    void delete_shouldReturn204() throws Exception {
        doNothing().when(exampleService).delete(1L);

        mockMvc.perform(delete("/api/v1/examples/1"))
                .andExpect(status().isNoContent());
    }
}
```

---

## 3. Repository Layer — JPA Slice Tests

- Use `@DataJpaTest` — loads only JPA context, no web layer
- Use Testcontainers for realistic DB testing (MySQL) or H2 for speed
- Test custom query methods — not `findById`/`save` (those are Spring's responsibility)
- Use `@BeforeEach` to seed test data via the repository itself
- Use `@Transactional` is already applied by `@DataJpaTest`

**Template (H2 in-memory):**
```java
@DataJpaTest
class ExampleRepositoryTest {

    @Autowired
    private ExampleRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.save(ExampleEntity.builder().name("alice").build());
        repository.save(ExampleEntity.builder().name("bob").build());
    }

    @Test
    @DisplayName("findByName() - should return entity when name exists")
    void findByName_shouldReturnEntity_whenExists() {
        Optional<ExampleEntity> result = repository.findByName("alice");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("alice");
    }

    @Test
    @DisplayName("findByName() - should return empty when name not found")
    void findByName_shouldReturnEmpty_whenNotFound() {
        Optional<ExampleEntity> result = repository.findByName("unknown");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save() - should persist entity with generated ID")
    void save_shouldPersistWithId() {
        ExampleEntity entity = ExampleEntity.builder().name("charlie").build();

        ExampleEntity saved = repository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("charlie");
    }
}
```

**Template (Testcontainers — MySQL):**
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ExampleRepositoryContainerTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private ExampleRepository repository;

    @Test
    @DisplayName("findByName() - should work with real MySQL")
    void findByName_shouldWork_withRealMySQL() {
        repository.save(ExampleEntity.builder().name("container-test").build());

        Optional<ExampleEntity> result = repository.findByName("container-test");

        assertThat(result).isPresent();
    }
}
```

---

## 4. Mapper Layer — Unit Tests

- Pure unit tests — no Spring context, no mocks needed
- Test `toEntity()`, `toResponse()`, and `toResponseList()`
- Assert every field is mapped correctly — do not skip fields
- Test null-safety where applicable

**Template:**
```java
@ExtendWith(MockitoExtension.class)
class ExampleMapperTest {

    private final ExampleMapper mapper = new ExampleMapper();

    @Test
    @DisplayName("toEntity() - should map all fields from request to entity")
    void toEntity_shouldMapAllFields() {
        ExampleRequest request = new ExampleRequest("test-name");

        ExampleEntity entity = mapper.toEntity(request);

        assertThat(entity.getName()).isEqualTo("test-name");
        assertThat(entity.getId()).isNull(); // ID not set on mapping
    }

    @Test
    @DisplayName("toResponse() - should map all fields from entity to response")
    void toResponse_shouldMapAllFields() {
        ExampleEntity entity = ExampleEntity.builder()
                .id(1L)
                .name("test-name")
                .createdAt(LocalDateTime.now())
                .build();

        ExampleResponse response = mapper.toResponse(entity);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("test-name");
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("toResponseList() - should map list of entities to list of responses")
    void toResponseList_shouldMapList() {
        List<ExampleEntity> entities = List.of(
                ExampleEntity.builder().id(1L).name("a").build(),
                ExampleEntity.builder().id(2L).name("b").build()
        );

        List<ExampleResponse> responses = mapper.toResponseList(entities);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getName()).isEqualTo("a");
        assertThat(responses.get(1).getName()).isEqualTo("b");
    }

    @Test
    @DisplayName("toResponseList() - should return empty list for empty input")
    void toResponseList_shouldReturnEmptyList_forEmptyInput() {
        List<ExampleResponse> responses = mapper.toResponseList(Collections.emptyList());

        assertThat(responses).isEmpty();
    }
}
```

---

## 5. Integration Tests — Full Spring Context

- Use `@SpringBootTest` + `@AutoConfigureMockMvc` — loads everything
- Use for end-to-end flow: HTTP request → Controller → Service → Repository → DB
- Use Testcontainers for real DB
- Use `@Sql` annotation or `@BeforeEach` for seeding
- Ideal for smoke tests and regression coverage

**Template:**
```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ExampleIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExampleRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("POST then GET - should persist and retrieve resource")
    void createThenGet_shouldPersistAndRetrieve() throws Exception {
        ExampleRequest request = new ExampleRequest("integration-test");

        // Create
        String responseBody = mockMvc.perform(post("/api/v1/examples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(responseBody).get("id").asLong();

        // Retrieve
        mockMvc.perform(get("/api/v1/examples/" + createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("integration-test"));
    }
}
```

---

## Required Maven Dependencies

Add to `pom.xml` if not already present:

```xml
<!-- JUnit 5 + Mockito — included via spring-boot-starter-test -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Testcontainers -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <scope>test</scope>
</dependency>

<!-- AssertJ — included via spring-boot-starter-test -->
<!-- Mockito — included via spring-boot-starter-test -->
```

---

## Generation Behaviour

1. **Ask for the layer** if not specified: "Which layer do you want tests for — Controller, Service, Repository, or Mapper?"
2. **Ask for the entity name and fields** if not provided — mirror the exact field names from the user's code
3. **Read the user's code before generating** — do not guess method signatures, field names, or exception types
4. **Generate one layer at a time** unless user asks for all
5. **Always include at least one negative/edge case** per method — not just happy paths
6. **After generating**, offer to generate the next layer: "Want me to generate the Controller tests next?"
7. **Name test methods descriptively**: `methodName_shouldDoX_whenConditionY`
8. **Use `@DisplayName`** on every test — makes test reports human-readable
9. If the user's service throws a specific exception class, **use that exact class** in `assertThatThrownBy`
10. If the project uses **JWT security**, add a note to use `@WithMockUser` on `@WebMvcTest` tests or disable security for the slice

---

## Test Naming Convention

Always follow this pattern:
```
methodUnderTest_shouldExpectedBehaviour_whenCondition
```

Examples:
- `create_shouldReturnResponse_whenValidRequest`
- `getById_shouldThrowException_whenNotFound`
- `delete_shouldCallDeleteById_whenEntityExists`
- `findByName_shouldReturnEmpty_whenNameNotFound`