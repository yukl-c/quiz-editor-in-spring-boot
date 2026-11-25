# Quiz

A Spring Boot web app for managing quiz questions (admin) and taking quizzes (user). Pages are Thymeleaf templates with Spring Security login, roles, and custom 403/404 pages.

## Requirements

- **JDK 21**
- No global Maven install is required. Use the wrapper (`mvnw.cmd` on Windows, `./mvnw` on macOS/Linux).

## Run

From the project root (`D:\Spring\quiz` or wherever you cloned it).

**Windows PowerShell** — quote `-D` arguments so PowerShell does not strip them:

```powershell
.\mvnw.cmd clean install
.\mvnw.cmd spring-boot:run
```

Skip tests:

```powershell
.\mvnw.cmd clean install -DskipTests
.\mvnw.cmd spring-boot:run
```

**macOS / Linux:**

```bash
./mvnw clean install
./mvnw spring-boot:run
```

Open [http://localhost:8080/login](http://localhost:8080/login).

If port **8080** is already in use:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

Do not pass unquoted `-Dspring-boot.run.mainClass=...` in PowerShell; Maven will treat `.mainClass=...` as a lifecycle phase.

## First use

There are no built-in accounts. Users and questions are stored **in memory** (`HashMap`) and are **cleared when the process stops**.

1. Go to [http://localhost:8080/register](http://localhost:8080/register).
2. Create an **ADMIN** account and a **USER** account.
3. Log in. After login you are sent to `/home`, which redirects:
   - **ADMIN** → `/quiz-list` (add / edit / delete questions)
   - **USER** → `/quiz` (answer questions and submit)

## Project functions

The app is a small **Spring MVC** quiz product: Thymeleaf views, a `@Controller`, `@Service` beans, and Spring Security in front of every request.

### Account and session

| Function | How it works in Spring |
|----------|------------------------|
| **Register** | `GET/POST /register`. The controller calls `QuizUserDetailsService.registerUser`, which stores a domain `User` and hashes the password with **BCrypt**. Duplicate usernames fail with `redirect:/register?error`. |
| **Login** | Custom `loginPage("/login")`. Spring Security’s form login filter posts to `/login`, loads the user via **`UserDetailsService.loadUserByUsername`**, and checks the hash with the `PasswordEncoder` bean. Success goes to `/home` (`defaultSuccessUrl(..., true)` always, not only the saved request). |
| **Logout** | `POST /logout` from Thymeleaf (`th:action` so the **CSRF** token is included). The security filter clears the `SecurityContext` and session. |
| **Home router** | `GET /home` is authenticated. The controller reads `SecurityContextHolder.getContext().getAuthentication()`, inspects `GrantedAuthority` values, and **redirects** to `/quiz-list` or `/quiz`. Templates are never opened as `/Quiz.html`; the return value `"Quiz"` is a **view name**. |

### Admin: question bank (CRUD)

| Function | Mapping | Notes |
|----------|---------|--------|
| List questions | `GET /quiz-list` | View `QuizList.html`. Data from `QuestionsService.getQuizzesList()`. |
| Add question | `GET/POST /addQuiz` | Binds a `Quiz` with `@ModelAttribute`. Assigns `getNextId()`, then `addQuiz`. |
| Edit question | `GET /editQuiz/{id}`, `POST /editQuestion` | Path variable loads the entity; missing id redirects with `?error`. |
| Delete question | `GET /deleteQuiz/{id}` | Removes by id. (Prefer POST in a production app because CSRF does not protect GET.) |

### User: take the quiz

| Function | Mapping | Notes |
|----------|---------|--------|
| Take quiz | `GET /quiz` | View `Quiz.html`. Radios named `answer{id}` so scoring is by **question id**, not list index. |
| Submit answers | `POST /submitQuiz` | Controller builds a list of `Quiz` objects (id + user’s answer in `correctAnswer`) and calls **`QuestionsService.submitQuiz`**, which compares against the stored bank. Result view: `result.html` (score and per-question review). |

### Errors

| Function | Mapping | Notes |
|----------|---------|--------|
| Forbidden | `/403` | `accessDeniedPage("/403")` plus a servlet **ErrorPage** for HTTP 403. |
| Not found | `/404` | Servlet **ErrorPage** for HTTP 404; `ErrorPageController` returns `error/404`. |
| Whitelabel off | `server.error.whitelabel.enabled=false` | App templates are used instead of Boot’s default error page. |

The `quizErrorPageCustomizer` bean name is intentional: Boot’s `ErrorMvcAutoConfiguration` already defines `errorPageCustomizer`. Reusing that name causes `BeanDefinitionOverrideException` and `contextLoads` fails.

---

## Role-based access (Spring Security)

Authorization is **declarative on URLs** in `SecurityFilterChain`, then **optionally checked again** in the controller. That is the usual Spring pattern: the filter chain is the gate; the controller uses `Authentication` for navigation.

### Roles vs authorities

On register the form sends `ADMIN` or `USER` (no prefix). `User.builder().roles(role)` **prepends** `ROLE_`, so the `GrantedAuthority` is `ROLE_ADMIN` or `ROLE_USER`.

In `authorizeHttpRequests`, **`hasRole("ADMIN")` looks for `ROLE_ADMIN`**. Do not store `ROLE_ADMIN` in the database/map and then call `.roles(...)` — you would get `ROLE_ROLE_ADMIN` and every `hasRole` check would fail.

`hasRole` is **not** hierarchical: `ADMIN` is not a `USER`. An admin calling `/quiz` or `/submitQuiz` receives **403**, and a user calling `/quiz-list` receives **403**. That is by design in this project.

### Filter chain (order matters)

```text
permitAll     → /register, /login, /error, /403, /404
hasRole ADMIN → /quiz-list, /addQuiz, /editQuiz/**, /editQuestion, /deleteQuiz/**
hasRole USER  → /quiz, /submitQuiz
authenticated → everything else (including /home)
```

Spring Security evaluates **first matching** `requestMatchers` rule. Public pages must be listed before `anyRequest().authenticated()`, or login itself would require a session.

Anonymous users hitting a protected URL are sent to `/login`. Authenticated users without the required role are sent to **`/403`**, not 404. 404 is only for URLs that have **no handler**.

### Two layers of role checks

1. **Filter (authoritative):** `WebSecurityConfig` — if this fails, the controller never runs.
2. **Controller (UX):** `/home` branches on `authentication.getAuthorities()`. Add/edit/delete also compare `ROLE_ADMIN` before mutating data.

`SecurityContextHolder` is the thread-local Spring uses after the security filter has authenticated the request. Constructor injection of `AuthenticationManager` is used on register to authenticate programmatically (the app still redirects to `/login?success`, so the user signs in with the form next).

### What each role can do

**Anonymous**

- Register, open the login form, see 403/404 templates.

**ROLE_USER**

- Log in, land on `/quiz`, submit answers, see results, log out.
- Cannot manage the question bank (`/quiz-list`, add/edit/delete) → **403**.

**ROLE_ADMIN**

- Log in, land on `/quiz-list`, create/update/delete questions, log out.
- Cannot take the quiz (`/quiz`, `/submitQuiz`) → **403**, because those matchers use `hasRole("USER")` only. To allow both roles you would use `hasAnyRole("ADMIN", "USER")`.

### MVC vs Security

Thymeleaf files under `src/main/resources/templates/` are **not** public static files. Spring MVC resolves a logical view name (`return "QuizList"`) through the Thymeleaf view resolver. Requesting `/QuizList.html` in the browser is a **404** unless you map that path.

CSRF is **on** by default. Forms must use `th:action` (or a hidden `_csrf` field). A raw HTML `action="/login"` without the token fails POST.

## Stack

- Spring Boot **4.2.0**
- Spring Security (form login, BCrypt passwords)
- Spring Web MVC + Thymeleaf
- In-memory user store (`QuizUserDetailsService`) and question store (`QuestionsService`)

## Project layout

```
src/main/java/com/app/quiz/
  config/          Security and error-page mapping
  controller/      Quiz, register, login, 403/404
  model/           User, Quiz
  service/         Users and questions
src/main/resources/templates/
  login.html, register.html, Quiz.html, QuizList.html, ...
  error/403.html, error/404.html
```

## Tests

```powershell
.\mvnw.cmd test
```

`QuizApplicationTests.contextLoads` starts the Spring context. If it fails with a duplicate bean `errorPageCustomizer`, the customizer bean in `ErrorPageConfig` must not use that name (use `quizErrorPageCustomizer`).
