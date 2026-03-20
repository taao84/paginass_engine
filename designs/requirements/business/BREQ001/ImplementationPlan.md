# BREQ001 — Implementation Plan: Basic Search Site

## Architecture Overview

```
User types query (Nuxt.js)
  → GET /api/search?q=...&page=0
  → SearchController
  → BusinessProfileService
  → BusinessProfileRepository (native SQL: tsvector @@ tsquery, ts_rank)
  → SearchResponseDto (paginated)
  → JSON response to Nuxt.js frontend
```

---

## Step 1 — DB Migration `V2__add_search_vector.sql`

**File:** `src/main/resources/db/migration/V2__add_search_vector.sql`

Add a `tsvector` column to `business_profiles`, a trigger to auto-populate it, and a GIN index.

| Weight | Source Field | Rationale |
|--------|-------------|-----------|
| A | `name` | Highest relevance — exact business name matches |
| B | `description` | Descriptive text |
| C | `tags` JSONB | JSON syntax stripped before tokenization |
| D | `contact_info` JSONB | Lowest relevance — address, city, etc. |

A `BEFORE INSERT OR UPDATE` trigger keeps the vector automatically in sync on every write.

> **Note on language:** `'spanish'` is used as the text search configuration. If the target audience is English-speaking, change to `'english'` before running the migration — changing it afterwards requires rebuilding all vectors.

---

## Step 2 — `BusinessProfile` Entity

**File:** `src/main/java/com/paginass/entity/BusinessProfile.java`

New JPA entity mapping the `business_profiles` table. Key decisions:

- **JSONB columns** (`tags`, `contact_info`, `image_urls`): mapped with `@JdbcTypeCode(SqlTypes.JSON)` — Hibernate 6 native support, no extra dependency needed.
- **`search_vector`**: mapped as `insertable = false, updatable = false` — managed exclusively by the DB trigger, never touched by JPA. Required to satisfy `ddl-auto: validate`.
- **`biz_type`** PostgreSQL enum: mapped via `@Column(columnDefinition = "business_type")` + `@Enumerated(EnumType.STRING)`. Define a corresponding Java enum `BusinessType { BRICK_AND_MORTAR, INFORMAL, GIG }`.

---

## Step 3 — DTOs

**Directory:** `src/main/java/com/paginass/dto/`

### `BusinessProfileSummaryDto`
Result card data returned per search hit. Intentionally excludes `owner_id`, `image_urls`, `search_vector`, and audit timestamps.

| Field | Type | Notes |
|-------|------|-------|
| `id` | `UUID` | |
| `name` | `String` | |
| `slug` | `String` | |
| `bizType` | `String` | |
| `description` | `String` | |
| `tags` | `List<String>` | |
| `contactInfo` | `Map<String, String>` | |
| `score` | `double` | From `ts_rank` — used for ordering, not displayed to users |

### `SearchResponseDto`
Paginated wrapper around the result list.

| Field | Type |
|-------|------|
| `results` | `List<BusinessProfileSummaryDto>` |
| `page` | `int` |
| `totalPages` | `int` |
| `totalResults` | `long` |

---

## Step 4 — `BusinessProfileRepository`

**File:** `src/main/java/com/paginass/repository/BusinessProfileRepository.java`

Native PostgreSQL query using:
- `websearch_to_tsquery` — handles natural language input without requiring tsquery syntax from the user.
- `ts_rank` — for relevance scoring.
- `is_active = true` filter — only live profiles appear in results.
- Returns `Page<Object[]>` with a separate `countQuery` for pagination metadata.

---

## Step 5 — `BusinessProfileService`

**File:** `src/main/java/com/paginass/service/BusinessProfileService.java`

Responsibilities:
- Hardcoded page size of **10**.
- Builds `Pageable` from the requested page number.
- Maps `Object[]` rows to `BusinessProfileSummaryDto` using positional index (must match SELECT column order exactly).
- Uses `ObjectMapper` (available via `spring-boot-starter-web`) to deserialize JSONB string columns back to `List<String>` / `Map<String, String>`.
- Wraps result in `SearchResponseDto`.

**`Object[]` row mapping (must match native query SELECT order):**

| Index | Column | Type |
|-------|--------|------|
| 0 | `id` | String/UUID |
| 1 | `name` | String |
| 2 | `slug` | String |
| 3 | `biz_type` | String |
| 4 | `description` | String |
| 5 | `tags` | String (raw JSON) |
| 6 | `contact_info` | String (raw JSON) |
| 7 | `score` | Float/Double |

---

## Step 6 — `SearchController`

**File:** `src/main/java/com/paginass/controller/SearchController.java`

- `GET /api/search?q=...&page=0`
- Fully public — no authentication required.
- Returns `400 Bad Request` for blank/missing `q` parameter.
- All pagination via query parameters (no path segments), making results bookmark-able.

---

## Step 7 — Update `SecurityConfig`

**File:** `src/main/java/com/paginass/config/SecurityConfig.java`

Add `/api/search` to the `permitAll` matcher list. Minimal, targeted change — no other security behavior is affected.

---

## Step 8 — `CorsConfig`

**File:** `src/main/java/com/paginass/config/CorsConfig.java`

Allow `http://localhost:3000` (Nuxt dev server) to call `/api/**` with `GET` requests. In production, the allowed origin should be driven by an `application.yml` property.

---

## Step 9 — Nuxt.js Project Init

**Directory:** `frontend/` (at project root, alongside `src/`)

- Initialize Nuxt 3 project: `npx nuxi@latest init frontend`
- Install Tailwind: `npm install -D @nuxtjs/tailwindcss`
- Add `@nuxtjs/tailwindcss` to `modules` in `nuxt.config.ts`
- Configure `runtimeConfig.public.apiBase` pointing to the backend URL (`http://localhost:8080` for dev)

---

## Step 10 — Frontend Components & Pages

**Directory:** `frontend/`

| File | Purpose |
|------|---------|
| `composables/useSearch.ts` | API call logic + reactive state (`results`, `totalPages`, `isLoading`, `error`) |
| `components/SearchBox.vue` | Text input + button; submits on Enter key or button click |
| `components/ResultCard.vue` | Displays name, type badge, description snippet, tag pills, contact info |
| `components/Pagination.vue` | Prev/Next + page number controls |
| `pages/index.vue` | Homepage — centered search box only; navigates to `/search?q=...` on submit |
| `pages/search.vue` | Results page — reads `?q=` & `?page=` from URL, calls composable, renders list |

### `useSearch.ts` — Exposed Interface

```ts
interface BusinessProfileSummary {
  id: string
  name: string
  slug: string
  bizType: string
  description: string
  tags: string[]
  contactInfo: Record<string, string>
  score: number
}
```

Exposes: `results`, `totalPages`, `currentPage`, `isLoading`, `error`, `search(query, page)`.

### `pages/index.vue` Layout
```
[Site title / logo]
[SearchBox — centered on screen]
```

### `pages/search.vue` Layout
```
[SearchBox — pre-filled with current query]
[Loading spinner | Error message | "No results found"]
[ResultCard × N]
[Pagination]
```

---

## Implementation Order

```
Step 1  V2 Migration
  └─ Step 2  BusinessProfile Entity
       ├─ Step 3  DTOs
       └─ Step 4  Repository
            └─ Step 5  Service
                 ├─ Step 6  SearchController
                 ├─ Step 7  SecurityConfig update
                 └─ Step 8  CorsConfig

Step 9  Nuxt.js Init  (independent — can start any time)
  └─ Step 10  useSearch composable → Components → Pages
```

**Recommended order for a single developer:**
1. V2 Migration
2. `BusinessProfile` entity + `BusinessType` enum
3. DTOs
4. Repository
5. Service
6. Controller
7. SecurityConfig + CorsConfig
8. Manual backend smoke test: `curl "http://localhost:8080/api/search?q=test"`
9. Nuxt project init
10. `useSearch` composable
11. Components (bottom-up): `ResultCard` → `Pagination` → `SearchBox` → `search.vue` → `index.vue`

---

## Pitfalls & Notes

| Risk | Detail                                                                                                                                                                |
|------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Language consistency** | `'spanish'` must match in both the trigger and the query. A mismatch causes zero results.                                                                             |
| **Schema validation** | `ddl-auto: validate` will fail if `search_vector` exists in the DB but is not mapped on the entity. The `insertable=false, updatable=false` mapping is required.      |
| **`Object[]` row mapping** | Positional index mapping in the service is brittle — it must exactly match the SELECT column order in the native query. Any reordering breaks it silently.            |
| **Nuxt SSR** | Use the full `apiBase` URL from `runtimeConfig.public` for all `$fetch` calls. Relative URLs fail during server-side rendering.                                       |
| **`biz_type` enum mapping** | If Hibernate struggles with the native PostgreSQL enum, fall back to mapping `bizType` as `String` in the entity and converting in the service.                       |
| **JSONB tag tokenization** | The regex `[\[\]",]` in the trigger strips JSON array syntax so `["plumber","pipe-repair"]` becomes `plumber pipe-repair` before tokenization. Verify with real data. |
