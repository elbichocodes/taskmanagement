In-Depth Project Report: Task Management System
This report provides a granular analysis of the Task Management System project, detailing the implementation of both the frontend (React/Vite) and backend (Java/Spring Boot) components.

I. Project Architecture & Core Concepts
Pattern: Client-Server Web Application.
Frontend: Single Page Application (SPA) responsible for UI rendering and user interaction. Communicates with the backend via HTTP requests.
Backend: RESTful API providing data persistence, business logic, and security.
Communication: Stateless HTTP protocol. The frontend sends requests (GET, POST, PUT, DELETE) to backend endpoints. Authentication is handled via JSON Web Tokens (JWT).
Data Flow (Example: User Login):
User enters credentials in the React Login form (pages/Login.jsx).
On submit, the form handler makes a POST request to the backend's /auth/login endpoint, sending email and password.
The backend's AuthController receives the request.
Spring Security's AuthenticationManager validates the credentials using UserDetailsServiceImpl (which fetches user data via UserRepository) and PasswordEncoder.
If successful, JwtUtil generates a JWT containing user details (like email/username) and an expiration time, signed with a secret key.
AuthController returns a JwtResponse containing the JWT to the frontend.
The React app stores the JWT (typically in localStorage).
For subsequent requests to protected endpoints (e.g., fetching tasks), the React app includes the JWT in the Authorization: Bearer <token> header.
The backend's JwtRequestFilter intercepts the request, validates the token using JwtUtil, extracts user details, and sets the security context, allowing access to the protected resource.
II. Frontend Deep Dive (code folder/front end/)
A. Setup & Build (package.json, vite.config.js, tailwind.config.js)

package.json:
dependencies: Lists runtime libraries:
react, react-dom: Core React library for building UI components.
react-router-dom: Handles client-side routing within the SPA.
tailwindcss: Utility-first CSS framework for styling.
devDependencies: Lists build/development tools:
vite, @vitejs/plugin-react: Fast build tool and development server optimized for modern frameworks like React.
autoprefixer, postcss: Process CSS, primarily for Tailwind compatibility.
eslint: Code linter for maintaining code quality (configured in eslint.config.js).
scripts:
dev: Starts the Vite development server (likely with Hot Module Replacement).
build: Creates an optimized production build of the frontend assets.
preview: Serves the production build locally for testing.
vite.config.js: (Appears empty in provided files) - Usually configures Vite plugins, server options (port, proxy), build options, etc. If empty, it uses Vite's defaults.
tailwind.config.js: Configures Tailwind CSS, defining theme customizations (colors, fonts), content paths (files Tailwind should scan for classes), and plugins.
B. Application Entry & Routing (main.jsx, App.jsx)

main.jsx:
Imports necessary libraries (React, ReactDOM, BrowserRouter).
Imports the main App component and the main CSS (index.css).
Uses ReactDOM.createRoot().render() to mount the React application into the div#root element specified in index.html.
Wraps the App component within <BrowserRouter> to enable client-side routing using the HTML5 history API.
App.jsx:
The root component of the application.
Imports page components (Home, Login, Signup, Dashboard, ForgotPassword, ResetPassword).
Uses Routes and Route components from react-router-dom to define application paths and the corresponding components to render.
Example Route: <Route path="/login" element={<Login />} /> maps the /login URL path to the Login component.
Protected Route Logic (Conceptual): Although not explicitly shown in App.jsx structure, protecting the /dashboard route typically involves creating a wrapper component. This wrapper checks if a valid JWT exists in localStorage. If yes, it renders the Dashboard component; otherwise, it redirects the user to the /login page using the Maps component from react-router-dom.
C. Page Components (src/pages/)

General Structure: Each page component is a React function component.
Uses useState hook to manage component-local state (e.g., form inputs, loading status, error messages, fetched data).
Uses useEffect hook for side effects, primarily fetching data from the backend when the component mounts (e.g., fetching tasks in Dashboard.jsx).
Uses event handlers (e.g., onSubmit, onChange, onClick) to respond to user interactions.
Makes API calls using Workspace to backend endpoints.
Uses useNavigate hook from react-router-dom for programmatic navigation (e.g., redirecting to dashboard after login).
Uses Tailwind CSS classes extensively for styling HTML elements.
Login.jsx:
State: email, password, rememberMe, error, loading.
Handles form input changes.
handleSubmit: Prevents default form submission, sets loading state, makes a POST request to http://localhost:8080/auth/login with email and password.
Handles response: If successful (status 200), parses JWT from response, stores it in localStorage, potentially stores email in localStorage if rememberMe is checked, and navigates to /dashboard. If error, sets the error state.
Signup.jsx:
State: email, password, error, loading.
Similar structure to Login.jsx, but makes a POST request to /auth/register. Navigates to /login on successful registration.
Dashboard.jsx:
State: tasks (array), newTaskTitle, editingTaskId, editingTaskTitle, error, loading.
useEffect: Fetches tasks from /api/tasks when the component mounts. Requires including the JWT in the Authorization header (likely using a helper function WorkspaceWithAuth). Sets the tasks state.
Task Rendering: Maps over the tasks array to display each task. Includes Edit and Delete buttons. Implements conditional rendering to show an input field when a task is being edited (editingTaskId).
handleAddTask: Makes a POST request to /api/tasks with the newTaskTitle. Updates the tasks state on success.
handleEditTask: Makes a PUT request to /api/tasks/{id} with the updated title. Updates the tasks state.
handleDeleteTask: Makes a DELETE request to /api/tasks/{id}. Updates the tasks state by filtering out the deleted task.
handleLogout: Removes the JWT from localStorage and navigates to /login.
ForgotPassword.jsx:
State: email, message, error, loading.
handleSubmit: Makes a POST request to /auth/forgot-password with the email. Displays success/error messages.
ResetPassword.jsx:
State: password, confirmPassword, message, error, loading.
useEffect: Extracts the token from the URL query parameters on mount.
handleSubmit: Makes a POST request to /auth/reset-password including the token and the new password. Handles success/error responses and may navigate to /login.
D. Styling (index.css, App.css, Tailwind)

index.css: Includes Tailwind base styles, components, and utilities. May contain global custom styles.
App.css: Contains specific CSS rules for the App component or globally applied styles not covered by Tailwind utilities.
Tailwind classes are applied directly within the JSX elements for rapid UI development (e.g., <div className="flex items-center justify-between p-4 bg-blue-500 text-white">).
III. Backend Deep Dive (code folder/backend/)
A. Project Setup (pom.xml, TaskmanagementApplication.java)

pom.xml (Maven Build File):
parent: Inherits defaults from spring-boot-starter-parent (dependency management, plugin configurations).
dependencies: Defines project libraries:
spring-boot-starter-web: For building RESTful web applications (includes Tomcat, Spring MVC).
spring-boot-starter-data-jpa: For database interaction using Java Persistence API (includes Hibernate).
spring-boot-starter-security: For handling authentication and authorization.
spring-boot-starter-mail: For sending emails.
ojdbc11: Oracle database JDBC driver.
h2: H2 in-memory database driver (for testing).
com.auth0:java-jwt: Library for creating and verifying JWTs.
org.projectlombok:lombok: Reduces boilerplate code (getters, setters, constructors) via annotations.
springdoc-openapi-starter-webmvc-ui: Integrates Swagger UI for API documentation.
spring-boot-starter-test: Utilities for testing Spring Boot applications (includes JUnit 5, Mockito).
build: Configures Maven plugins, notably spring-boot-maven-plugin for creating executable JARs.
TaskmanagementApplication.java:
@SpringBootApplication: A convenience annotation combining @Configuration, @EnableAutoConfiguration, and @ComponentScan. It tells Spring Boot to:
Enable auto-configuration based on classpath dependencies.
Scan the current package (com.taskmanager) and sub-packages for components (@Controller, @Service, @Repository, @Component, etc.).
Allow registering extra beans via @Bean methods.
main method: Standard Java entry point, uses SpringApplication.run() to launch the Spring Boot application.
B. Configuration (application.properties, CorsConfig.java, SecurityConfig.java)

application.properties (Main):
server.port=8080: Configures the embedded Tomcat server port.
spring.datasource.url, username, password, driver-class-name: Database connection details for Oracle. (Security Note: Hardcoding credentials is not recommended for production.)
spring.jpa.hibernate.ddl-auto=update: Hibernate automatically updates the database schema based on entity definitions. validate or none are safer for production.
spring.jpa.show-sql=true: Logs executed SQL statements (useful for debugging).
jwt.secret=...: The secret key used to sign and verify JWTs. (Security Note: Should be externalized and strong.)
jwt.expiration=...: Token validity duration.
spring.mail.host, port, username, password: SMTP server configuration for sending emails. (Security Note: Password should be externalized.)
springdoc.api-docs.path, swagger-ui.path: Configures paths for OpenAPI specification and Swagger UI.
application.properties (Test):
Overrides database configuration to use H2 in-memory database (jdbc:h2:mem:testdb).
spring.jpa.defer-datasource-initialization=true: Ensures data.sql runs after Hibernate schema creation.
Disables Oracle driver (driver-class-name=org.h2.Driver).
CorsConfig.java:
Implements WebMvcConfigurer.
@Configuration: Marks this as a Spring configuration class.
addCorsMappings method: Configures Cross-Origin Resource Sharing (CORS) globally. Allows requests from http://localhost:5173 (likely the frontend dev server) for all paths (/**), permits common HTTP methods (GET, POST, PUT, DELETE, etc.), allows credentials, and permits all headers. Necessary for the frontend SPA to communicate with the backend API running on a different origin (port).
SecurityConfig.java:
@Configuration, @EnableWebSecurity: Enables Spring Security's web security support.
@Bean public PasswordEncoder passwordEncoder(): Defines a BCryptPasswordEncoder bean for securely hashing passwords.
@Bean public AuthenticationProvider authenticationProvider(): Configures the authentication mechanism. Uses a DaoAuthenticationProvider which relies on a UserDetailsService (UserDetailsServiceImpl) to load user data and a PasswordEncoder to verify passwords.
@Bean public AuthenticationManager authenticationManager(...): Exposes the AuthenticationManager as a bean, needed by AuthController for programmatic login.
@Bean public SecurityFilterChain securityFilterChain(...): The core security configuration method.
csrf(AbstractHttpConfigurer::disable): Disables Cross-Site Request Forgery protection. Common for stateless REST APIs where token-based authentication provides protection, but understand the implications.
authorizeHttpRequests(...): Defines authorization rules:
requestMatchers("/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll(): Allows unauthenticated access to authentication endpoints, Swagger UI, and OpenAPI docs.
anyRequest().authenticated(): Requires authentication for all other requests.
sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)):1 Configures session management to2 be stateless, as authentication is handled per-request via JWT, not server-side sessions
authenticationProvider(authenticationProvider()): Registers the custom authentication provider.
addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class): Inserts the custom JwtRequestFilter before the standard filter that processes username/password authentication. This ensures the JWT is checked first for incoming requests.
C. Security Implementation (JwtUtil.java, JwtRequestFilter.java, UserDetailsServiceImpl.java)

UserDetailsServiceImpl.java:
Implements Spring Security's UserDetailsService.
@Service: Marks this as a Spring service bean.
Injects UserRepository.
loadUserByUsername(String email): Method required by the interface. Finds a User entity by email using userRepository.findByEmail. If not found, throws UsernameNotFoundException. If found, maps the User entity to a Spring Security UserDetails object (using the built-in org.springframework.security.core.userdetails.User implementation), providing the email (as username), password hash, and authorities (roles).
JwtUtil.java (or similar utility class):
@Component: Marks this as a Spring component bean.
Reads jwt.secret and jwt.expiration from properties (@Value).
generateToken(UserDetails userDetails): Creates a JWT using com.auth0.jwt.JWT. Sets the subject (username/email), issued-at time, expiration time (current time + configured expiration), and signs it using HMAC256 algorithm with the secret key.
validateToken(String token, UserDetails userDetails): Checks if the token is valid. Verifies the signature using the secret key, checks if the token has expired, and confirms the username in the token matches the UserDetails.
extractUsername(String token): Parses the token to extract the username (subject claim).
Helper methods to extract claims, check expiration, etc.
JwtRequestFilter.java:
Extends OncePerRequestFilter (ensures it runs once per request).
@Component: Marks this as a Spring component bean.
Injects JwtUtil and UserDetailsService.
doFilterInternal(...): Core logic executed for each request.
Extracts the Authorization header.
Checks if the header exists and starts with "Bearer ".
If yes, extracts the token string.
Extracts the username (email) from the token using jwtUtil.extractUsername.
Checks if a username was extracted and if there's no existing authentication in the SecurityContextHolder.
Loads UserDetails using userDetailsService.loadUserByUsername.
Validates the token using jwtUtil.validateToken.
If the token is valid: Creates a UsernamePasswordAuthenticationToken (representing the authenticated user) with UserDetails, null credentials (as it's token-based), and authorities. Sets details from the request (e.g., IP address).
Sets this Authentication object in the SecurityContextHolder.getContext(). This step signifies that the current user is authenticated for this request.
Calls filterChain.doFilter(request, response) to pass the request down the filter chain.
D. Controllers (AuthController.java, TaskController.java)

General Structure:
@RestController: Combines @Controller and @ResponseBody, indicating that return values should be bound to the web response body (typically as JSON).
@RequestMapping: Defines base path for all endpoints in the controller (e.g., /auth, /api/tasks).
Uses annotations like @PostMapping, @GetMapping, @PutMapping, @DeleteMapping to map HTTP methods and paths to specific handler methods.
Injects necessary services (TaskService), utilities (JwtUtil), and Spring components (AuthenticationManager).
Uses @RequestBody to bind incoming JSON request data to Java objects (payloads/DTOs).
Uses @PathVariable to extract values from the URL path.
Uses ResponseEntity to customize the HTTP response status and body.
Uses Principal (from java.security) or @AuthenticationPrincipal (Spring Security) in method arguments to get details about the currently authenticated user.
AuthController.java:
Handles authentication-related requests.
/login: Takes LoginRequest, uses authenticationManager.authenticate, generates JWT using jwtUtil, returns JwtResponse.
/register: Takes RegisterRequest, creates a new User (hashes password using passwordEncoder), saves via userRepository, returns success message. (Should ideally use a UserService).
/forgot-password: Takes ForgotPasswordRequest, finds user by email, generates a PasswordResetToken, saves it, sends reset email via emailService.
/reset-password: Takes ResetPasswordRequest, validates the token, finds user associated with token, updates user's password (hashed), deletes the token.
TaskController.java:
Handles CRUD operations for tasks, base path /api/tasks.
Requires authentication (due to SecurityConfig).
@GetMapping: Fetches all tasks for the authenticated user (using Principal to get username/email, then passing to taskService).
@PostMapping: Takes a Task object in the request body, associates it with the authenticated user, saves via taskService.
@PutMapping("/{id}"): Takes task ID from path and updated Task object from body, updates via taskService.
@DeleteMapping("/{id}"): Takes task ID from path, deletes via taskService.
E. Services (TaskService.java, EmailServiceImpl.java)

General Structure:
@Service: Marks class as a Spring service bean, encapsulating business logic.
Injects repositories (TaskRepository, UserRepository) or other services/utilities.
Provides methods called by controllers to perform operations.
TaskService.java:
Contains logic for finding, saving, updating, deleting tasks.
Interacts with TaskRepository and potentially UserRepository to ensure tasks are linked to the correct user.
EmailServiceImpl.java:
Implements EmailService interface.
Injects JavaMailSender.
sendEmail(String to, String subject, String text): Creates a SimpleMailMessage, sets recipient, subject, text, and uses mailSender.send() to dispatch the email via the configured SMTP server.
F. Data Layer (Entities & Repositories)

Entities (User.java, Role.java, Task.java, PasswordResetToken.java):
Plain Old Java Objects (POJOs) annotated for JPA (Java Persistence API).
@Entity: Marks the class as a JPA entity (corresponding to a database table).
@Table(name = "..."): Specifies the table name.
@Id: Marks the primary key field.
@GeneratedValue: Specifies primary key generation strategy (e.g., IDENTITY, SEQUENCE).
@Column: Maps fields to table columns, allows specifying constraints (name, length, nullable).
Relationship Annotations: @ManyToOne, @OneToMany, @ManyToMany define relationships between entities (e.g., a User has @OneToMany Tasks, a Task has a @ManyToOne User).
Lombok Annotations: @Data (generates getters, setters, toString, etc.), @NoArgsConstructor, @AllArgsConstructor reduce boilerplate.
Repositories (UserRepository.java, TaskRepository.java, etc.):
Interfaces extending Spring Data JPA's JpaRepository<EntityType, IdType>.
@Repository: Marks the interface as a Spring Data repository bean.
Inherits standard CRUD methods (save, findById, findAll, deleteById, etc.) from JpaRepository.
Allows defining custom query methods based on naming conventions (e.g., findByEmail(String email)) or using @Query annotation for complex JPQL or native SQL queries. Spring Data automatically provides the implementation.
G. Testing (TaskmanagementApplicationTests.java, data.sql, test/resources/application.properties)

TaskmanagementApplicationTests.java:
@SpringBootTest: Loads the full application context for integration testing.
contextLoads(): A basic test ensuring the Spring application context loads without errors.
Test Configuration:
Uses test/resources/application.properties which overrides the main properties, notably switching to the H2 database.
schema.sql (Optional): Can define the database schema if not relying solely on Hibernate's ddl-auto.
data.sql: Contains SQL INSERT statements to populate the H2 database with test data (e.g., sample users, roles, tasks) before tests run. This ensures a consistent state for testing.
