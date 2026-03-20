## Language Consistency

PostgreSQL full-text search uses a text search configuration (like 'spanish' or 'english') in two separate, independent places:
1. The trigger — when a business profile is saved, it calls to_tsvector('spanish', name) to build and store the search_vector.                                              2. The query — when a user searches, it calls websearch_to_tsquery('spanish', 'plomero') to build the search term.
   The configuration controls a stemming dictionary — it reduces words to their root form. In Spanish, 'plomeros' and 'plomero' both stem to the same root. So the stored      vector and the search query both produce that root, and they match.                                                                                                                                                                                                                                                                                     If they use different configurations, the stems won't align:                                                                                                                - Trigger stores vector using 'spanish' → stems 'plomeros' → 'plomer'
- Query uses 'english' → stems 'plomeros' → 'plomero' (different root)                                                                                                      - Result: zero matches, even though the data is there.
  This is silent — no error is thrown, the query just returns nothing. It's easy to miss during development if you're testing with single-word terms that happen to be the    same in both languages.                                                                                                                                                                                                                                                                                                                                 ---
  
## Object[] Row Mapping

Because the native query uses a computed column (ts_rank(...) AS score) that has no corresponding JPA entity field, Spring Data can't auto-map results to a BusinessProfile object. It returns a raw Object[] per row instead.                                                                                                        
  
The service then maps by positional index — the order of columns in the SELECT clause:
```
SELECT b.id,          -- row[0]                                                                                                                                                    b.name,        -- row[1]                                                                                                                                                    b.slug,        -- row[2]
  b.biz_type,    -- row[3]                                                                                                                                                    b.description, -- row[4]
  b.tags,        -- row[5]
  b.contact_info,-- row[6]
  ts_rank(...)   -- row[7]  ← score                                                                                                                                    FROM business_profiles b ...
```

The danger is that this coupling is invisible to the compiler. If someone later changes the query — say, adds a column between slug and biz_type — the indexes silently shift and row[3] now returns the wrong value. No compile error, no runtime exception; you just get garbage data or a ClassCastException at runtime. 

The mitigation is discipline:

Treat the SELECT column order in that query as a contract and add a comment in both the repository query and the service mapper making the dependency explicit. A safer long-term alternative is a @SqlResultSetMapping with @ConstructorResult, which maps by column name instead of position, but that adds
boilerplate and is not necessary for the initial implementation.
