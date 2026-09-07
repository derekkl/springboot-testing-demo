# springboot-testing-demo

The Java/Spring Boot sibling to [dotnet-testing-demo](https://github.com/derekkl/dotnet-testing-demo) — same order-pricing API, same three testing layers, same OpenShift test-gated build pattern, adapted to idiomatic Java/Maven/Spring Boot conventions rather than translated line-for-line from C#.

Built on **Spring Boot 4.1.1** and **Java 21**. Spring Boot 4.x is a meaningfully newer major version than most existing tutorials cover (Spring Boot 3.5's OSS support ended June 30, 2026), so expect a slightly higher chance of a build-log surprise on first run than usual — the patterns used here (constructor injection, `@RestController`, `@SpringBootTest`) are basic and stable across the 3.x/4.x boundary, but this repo's C# sibling also needed a few real fixes on its first real build, and this one's had zero opportunity to compile anywhere before being handed off (see the note at the bottom).

---

## The three layers, in Java idiom

Unlike the .NET version's three separate test *projects*, Java/Maven draws this boundary with **naming convention + plugin choice** within a single module — this is the idiomatic way Java teams do it, not a simplification:

| Layer | Where it lives | What runs it | What it exercises |
|---|---|---|---|
| Unit | `order-api/src/test/java`, classes named `*Tests.java` | **Surefire** (`mvn test`) | `OrderCalculator` alone, `DiscountService` mocked via Mockito. No Spring context, no HTTP. |
| Integration | `order-api/src/test/java`, classes named `*IT.java` | **Failsafe** (`mvn verify`, after packaging) | The real Spring context and embedded Tomcat — routing, JSON binding, the real `DefaultDiscountService` bean — via `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`. Still in-process: no separate OS process. |
| Functional/E2E | Separate module: `order-api-functional-tests/` | Plain JUnit, run directly | The actual **packaged jar**, launched as its own OS process (or already deployed on OpenShift), hit purely over HTTP via Java's built-in `HttpClient`. This module has **no Maven dependency on `order-api` at all** — it only knows the JSON wire contract, parsed with Jackson (a general-purpose library, not the app's own code). |

Surefire's default include pattern (`**/*Tests.java`, etc.) does **not** match `*IT.java`, and Failsafe's default include pattern (`**/*IT.java`) is the reverse — that's what keeps unit and integration tests from double-running or getting mixed up, without needing separate modules for those two layers the way the functional layer genuinely needs.

## What about regression testing?

Same answer as the .NET version: it's not a fourth layer, it's the **practice** of running the tests you already have automatically on every change. Here, that's `mvn verify` (gates the container image build) plus a functional-test Job run against the live deployment — together, that combination *is* the regression suite.

---

## Running it on OpenShift (Red Hat Developer Sandbox)

Same shape as the .NET version, with lessons from building that one already baked in here (explicit Route TLS termination, `dockerfilePath` under `strategy.dockerStrategy`, and a build-tool arbitrary-UID fix — Maven's version of the same problem `dotnet`'s CLI hit).

### 1. Target your project

```bash
oc project <your-namespace>
```

### 2. Build and deploy the app (unit + integration tests gate this build)

```bash
oc apply -f openshift/01-build.yaml
oc start-build order-api-springboot --follow
```

Watch for `mvn verify` running inside the log — both Surefire and Failsafe execute here, and either failing stops the image from being produced.

```bash
oc apply -f openshift/02-deploy.yaml
oc get pods -l app=order-api-springboot -w
```

Once `1/1 Running`:

```bash
export ROUTE=$(oc get route order-api-springboot -o jsonpath='{.spec.host}')
curl -s https://$ROUTE/health
```

### 3. Build the functional test runner image

```bash
oc apply -f openshift/03-functional-test-build.yaml
oc start-build order-api-springboot-functional-tests --follow
```

### 4. Run the functional/E2E tests as a Job against the real deployed pod

```bash
oc create -f openshift/04-functional-test-job.yaml
oc get pods -l role=functional-test-run --sort-by=.metadata.creationTimestamp
oc logs -f <the-newest-pod-name>
```

`generateName` in the manifest means each `oc create -f` produces a fresh Job — run it as many times as you want. Clean up old runs with:

```bash
oc delete job -l role=functional-test-run
```

---

## Running it locally (if you have Java 21 + Maven)

**Unit tests:**
```bash
cd order-api
mvn test
```

**Unit + integration tests together:**
```bash
cd order-api
mvn verify
```

**Functional/E2E tests** — need a packaged jar first:
```bash
cd order-api && mvn -DskipTests package && cd ..
export ORDERAPI_JAR_PATH="$(pwd)/order-api/target/order-api.jar"
cd order-api-functional-tests
mvn test
```

**Run the app directly:**
```bash
cd order-api
mvn spring-boot:run
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"me","items":[{"sku":"WIDGET","quantity":4,"unitPrice":30.00}]}'
```

---

## Mapping this to an enterprise pipeline (ADO)

Same shape as the .NET version's mapping: three stages (build+test, deploy, post-deploy functional validation) map directly onto Azure DevOps pipeline stages, with `mvn verify` replacing `dotnet test` and JUnit XML reports published the same way `.trx` files would be.

---

## A note on how this was built

Unlike `npm-scan-demo`, there's no Maven or internet access to Maven Central available in the environment that built this — Java 21 is present, but nothing could actually be compiled or run before handing this off, the same situation as `dotnet-testing-demo`. Combined with Spring Boot 4.x being newer/less-trodden ground, expect this one to need at least one real build-log-driven fix, possibly more than its .NET sibling needed. That's fine — paste back whatever `oc start-build ... --follow` actually says, and it's straightforward to fix from there.
