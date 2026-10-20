# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
I would keep the warehouse style (ports, use cases, and a repository adapter) as the target, and I would not keep Panache Active Record for new code.

Store extends PanacheEntity and calls Store.persist() / Store.findById() from the JAX-RS resource. Product and Warehouse already use a repository. Active Record is short for a demo, but it couples HTTP handlers to JPA, makes "publish only after commit" harder to see, and pushes business rules into the resource. A repository (or the warehouse ports) keeps the same Panache implementation underneath while letting the resource depend on an interface.

I would not rewrite Store in one go. New behaviour goes through a repository or a use case. Store can move when the next change touches it. I would also stop duplicating the ExceptionMapper that both ProductResource and StoreResource declare for Exception; one mapper is enough and two of them make the chosen handler ambiguous.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
OpenAPI-first gives a contract other teams can review before the code exists, generates the resource interface and model, and makes response codes explicit (201 on create, 204 on archive, 400/404 on failure). The cost is the generated layer: awkward method names, a separate bean from the domain model, and a slower edit cycle when the yaml and the mapper drift apart. Code-first is faster for a small CRUD resource and stays in one class, but the contract lives only in annotations and tests, so clients and the server can diverge without anyone noticing.

I would use OpenAPI-first for Warehouse and for anything a legacy system or another service calls. I would keep code-first only for internal endpoints that have no external consumer. If this service is published as one API, I would move Product and Store to the spec as well so there is a single source of truth.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
I would spend the time on the rules that can corrupt data, not on getters or generated OpenAPI classes.

First, unit tests around the use cases with an in-memory store: duplicate business unit code, unknown location, max warehouses, summed capacity, stock versus capacity, and the replace rules (same stock, capacity large enough for the previous stock, archive-then-create). Those tests do not need Docker and fail fast. Second, a smaller set of endpoint tests that prove status codes, persistence, and that the legacy store call runs only after commit. Third, one packaged smoke test that lists and archives a warehouse.

I would keep JaCoCo on the code we own and exclude the generated client. A coverage number is a guardrail (around 80% on the monolith packages), not the goal. Every bug fix adds the missing case. I would not chase the last branches of the legacy temp-file gateway or of generated models.
```