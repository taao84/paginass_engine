# Project: Paginass Search & Directory

## Project Overview
A localized search platform for a small town allowing residents to find brick-and-mortar businesses, informal "gig" workers, and community advertisements (jobs, sales, help wanted). Each registered user manages a "Mini-Site" representing their presence on the platform.

## Core Tech Stack
- **Backend:** Java 21+ / Spring Boot 3.x
- **Security:** Spring Security with OAuth2.0 (Social Logins)
- **Primary Database:** PostgreSQL 18 (Source of Truth)
- **Search Engine:** Elasticsearch (for weighted scoring and relevance)
- **File Storage:** S3-compatible storage (for business images/promos)

## Architectural Principles
- **Source of Truth:** All persistent data lives in PostgreSQL. Elasticsearch is a secondary index synchronized with Postgres.
- **Data Flexibility:** Use `JSONB` for tags, contact information, and image metadata to allow for evolving business requirements without frequent schema migrations.
- **Search Strategy:** Search queries should hit Elasticsearch. Results are ranked by a "match rate" score based on tags, descriptions, and contact metadata.
- **Multi-Tenancy:** Each user owns a profile that acts as a standalone public "mini-site" rendered via a unique `slug`.

## Database Schema (PostgreSQL)
The system uses UUIDs for all primary keys. Key tables include:
- `users`: Identity and OAuth provider links.
- `business_profiles`: Main entity containing `JSONB` fields for `tags` and `contact_info`.
- `advertisements`: Classifieds with expiration dates and categories.
- `business_news`: Time-sensitive promotional posts/updates.

### JSONB Usage Pattern
- `tags`: Stored as a JSON array of strings `["tag1", "tag2"]`.
- `contact_info`: Stored as a flat JSON object `{ "phone": "...", "whatsapp": "..." }`.
- `image_urls`: Stored as a JSON array of strings representing S3 paths.

## Implementation Guide for AI Assistant
- **Java Persistence:** Use Spring Data JPA. Map `JSONB` columns using `hypersistence-utils` or native Hibernate 6+ `@JdbcTypeCode(SqlTypes.JSON)`.
- **DTOs:** Maintain a strict separation between Database Entities and API Transfer Objects.
- **Validation:** Ensure all `slug` fields are URL-sanitized and unique.
- **Search Sync:** When a `business_profile` is updated, trigger an event to re-index the document in Elasticsearch.

## Future Scope (Current Roadmap)
- Implementation of a real-time Auction system (currently out of scope, but schema is built with ACID compliance in mind).
