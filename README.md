# BTG Funds Management API - Prueba Tecnica Backend

API REST para la gestion de fondos de inversion de BTG Pactual. Permite a los clientes suscribirse a fondos voluntarios de pension (FPV) y fondos de inversion colectiva (FIC), cancelar suscripciones y consultar el historial de transacciones con paginacion.

---

## Tabla de Contenido

1. [Descripcion General](#descripcion-general)
2. [Stack Tecnologico](#stack-tecnologico)
3. [Arquitectura](#arquitectura)
4. [Modelo de Dominio](#modelo-de-dominio)
5. [API Endpoints](#api-endpoints)
6. [Modelo de Datos (DynamoDB)](#modelo-de-datos-dynamodb)
7. [Componente SQL](#componente-sql)
8. [Configuracion y Ejecucion Local](#configuracion-y-ejecucion-local)
9. [Infraestructura AWS](#infraestructura-aws)
10. [Pipeline CI/CD](#pipeline-cicd)
11. [Testing](#testing)
12. [Decisiones de Diseno](#decisiones-de-diseno)

---

## Descripcion General

La prueba tecnica consta de dos partes:

### Parte 1 - API REST (80%)

Microservicio que implementa las siguientes funcionalidades:

- **Suscripcion a fondos**: Un cliente puede vincularse a un fondo si su saldo es mayor o igual al monto minimo de vinculacion. Al suscribirse, el saldo del cliente se debita por el monto minimo del fondo.
- **Cancelacion de suscripcion**: Un cliente puede cancelar su vinculacion a un fondo. Al cancelar, el monto de la suscripcion se acredita de vuelta al saldo del cliente.
- **Historial de transacciones**: Consulta paginada de todas las transacciones (aperturas y cancelaciones) de un cliente.
- **Notificaciones**: Al realizar una transaccion, se envia una notificacion al cliente via email o SMS segun su preferencia, utilizando AWS SNS.

### Parte 2 - SQL (20%)

Consulta SQL que resuelve el siguiente problema: dado un esquema relacional con clientes, sucursales, productos, inscripciones, disponibilidad y visitas, obtener los nombres de los clientes que tienen inscrito algun producto disponible **solo** en las sucursales que visitan.

---

## Stack Tecnologico

| Componente | Tecnologia |
|---|---|
| Lenguaje | Java 21 (LTS) |
| Framework | Spring Boot 3.4.3 |
| Build | Gradle 8.12 |
| Base de datos | AWS DynamoDB (NoSQL, on-demand) |
| Notificaciones | AWS SNS (Email / SMS) |
| Contenedores | Docker (multi-stage, Alpine) |
| Orquestacion | AWS ECS Fargate |
| Load Balancer | AWS Application Load Balancer |
| CI/CD | AWS CodePipeline + CodeBuild |
| IaC | AWS CloudFormation |
| Documentacion API | OpenAPI 3 / Swagger UI (springdoc 2.8.5) |
| Testing | JUnit 5 + Mockito + AssertJ |

---

## Arquitectura

El proyecto sigue una **Arquitectura Hexagonal (Ports & Adapters)** con separacion estricta entre capas:

```
┌─────────────────────────────────────────────────────────┐
│                    Adapter Layer                         │
│                                                         │
│  ┌─────────────────┐          ┌──────────────────────┐  │
│  │  adapter/in/rest │          │   adapter/out/dynamo │  │
│  │  (Controllers)   │          │   (DynamoDB Adapters)│  │
│  │  (DTOs)          │          │   (Entities)         │  │
│  └────────┬─────────┘          └──────────┬───────────┘  │
│           │                               │              │
│           │         ┌─────────┐           │              │
│           │         │  config │           │              │
│           │         │  (Beans,│           │              │
│           │         │  AWS)   │           │              │
│           │         └─────────┘           │              │
│  ┌────────▼───────────────────────────────▼───────────┐  │
│  │                 adapter/out/sns                     │  │
│  │                 (SNS Notifications)                 │  │
│  └────────────────────────┬───────────────────────────┘  │
│                           │                              │
└───────────────────────────┼──────────────────────────────┘
                            │
          ┌─────────────────▼─────────────────┐
          │          Domain Layer              │
          │                                   │
          │  ┌───────────┐  ┌──────────────┐  │
          │  │  port/in   │  │   port/out   │  │
          │  │  (UseCases)│  │ (Repositories│  │
          │  │            │  │  Notif. Port)│  │
          │  └─────┬──────┘  └──────────────┘  │
          │        │                           │
          │  ┌─────▼──────┐  ┌──────────────┐  │
          │  │  usecase/   │  │   model/     │  │
          │  │  (Impl)     │  │ (Records,    │  │
          │  │             │  │  Enums)      │  │
          │  └─────────────┘  └──────────────┘  │
          │                                   │
          │  ┌─────────────────────────────┐   │
          │  │  exception/                 │   │
          │  │  (Domain Exceptions)        │   │
          │  └─────────────────────────────┘   │
          └───────────────────────────────────┘
```

### Estructura de paquetes

```
com.btg.funds
├── adapter
│   ├── in.rest              # Controllers REST + DTOs
│   └── out
│       ├── dynamo           # Adaptadores DynamoDB + Entities
│       └── sns              # Adaptador de notificaciones SNS
├── configuration            # Beans de Spring, AWS Config, OpenAPI
└── domain
    ├── exception            # Excepciones de dominio
    ├── model                # Records inmutables (Client, Fund, Subscription, Transaction)
    ├── port
    │   ├── in               # Interfaces de casos de uso (input ports)
    │   └── out              # Interfaces de repositorios y notificaciones (output ports)
    └── usecase              # Implementaciones de casos de uso
```

### Reglas de dependencia

- **Domain** no importa nada de Spring, AWS ni adapters. Solo usa `java.*`.
- **Adapters** importan interfaces del dominio (ports) + frameworks (Spring, AWS SDK).
- **Configuration** conecta adapters con ports mediante inyeccion de dependencias.

---

## Modelo de Dominio

### Entidades (Java Records inmutables)

#### Client
```java
record Client(String id, String name, String email, String phone,
              BigDecimal balance, NotificationPreference notificationPreference)
```
- `hasEnoughBalance(amount)`: valida si el saldo es suficiente
- `debit(amount)`: retorna nuevo Client con saldo debitado
- `credit(amount)`: retorna nuevo Client con saldo acreditado

#### Fund
```java
record Fund(String id, String name, BigDecimal minimumAmount, String category)
```

#### Subscription
```java
record Subscription(String clientId, String fundId, String fundName,
                    BigDecimal amount, Instant subscribedAt)
```

#### Transaction
```java
record Transaction(String transactionId, String clientId, String fundId,
                   String fundName, TransactionType type, BigDecimal amount,
                   Instant timestamp)
```

### Enumeraciones

- **TransactionType**: `APERTURA` (suscripcion) | `CANCELACION` (retiro)
- **NotificationPreference**: `EMAIL` | `SMS`

### Casos de uso

| Caso de uso | Descripcion | Reglas de negocio |
|---|---|---|
| `SubscribeToFundUseCase` | Suscribir cliente a un fondo | Cliente existe, fondo existe, no esta suscrito, saldo >= monto minimo. Debita saldo, crea suscripcion, registra transaccion APERTURA, notifica. |
| `CancelSubscriptionUseCase` | Cancelar suscripcion | Cliente existe, fondo existe, suscripcion existe. Acredita saldo, elimina suscripcion, registra transaccion CANCELACION, notifica. |
| `GetTransactionHistoryUseCase` | Consultar historial paginado | Cliente existe. Retorna `PageResult<Transaction>` con metadatos de paginacion. |

### Excepciones de dominio

| Excepcion | HTTP Status | Cuando se lanza |
|---|---|---|
| `ClientNotFoundException` | 404 | Cliente no encontrado |
| `FundNotFoundException` | 404 | Fondo no encontrado |
| `SubscriptionNotFoundException` | 404 | Suscripcion no existe al cancelar |
| `AlreadySubscribedException` | 409 | Cliente ya suscrito al fondo |
| `InsufficientBalanceException` | 400 | Saldo insuficiente para el monto minimo |

---

## API Endpoints

### Suscribirse a un fondo

```
POST /api/v1/funds/subscribe
Content-Type: application/json

{
  "clientId": "client-001",
  "fundId": "1"
}
```

**Respuesta exitosa (201 Created):**
```json
{
  "transactionId": "uuid-generado",
  "clientId": "client-001",
  "fundId": "1",
  "fundName": "FPV_BTG_PACTUAL_RECAUDADORA",
  "type": "APERTURA",
  "amount": 75000,
  "timestamp": "2026-03-16T12:00:00Z"
}
```

### Cancelar suscripcion

```
POST /api/v1/funds/cancel
Content-Type: application/json

{
  "clientId": "client-001",
  "fundId": "1"
}
```

**Respuesta exitosa (200 OK):**
```json
{
  "transactionId": "uuid-generado",
  "clientId": "client-001",
  "fundId": "1",
  "fundName": "FPV_BTG_PACTUAL_RECAUDADORA",
  "type": "CANCELACION",
  "amount": 75000,
  "timestamp": "2026-03-16T12:01:00Z"
}
```

### Consultar historial de transacciones

```
GET /api/v1/transactions/{clientId}?page=0&size=10
```

**Respuesta exitosa (200 OK):**
```json
{
  "content": [
    {
      "transactionId": "uuid",
      "clientId": "client-001",
      "fundId": "1",
      "fundName": "FPV_BTG_PACTUAL_RECAUDADORA",
      "type": "APERTURA",
      "amount": 75000,
      "timestamp": "2026-03-16T12:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

### Respuesta de error (formato estandar)

```json
{
  "status": 400,
  "error": "INSUFFICIENT_BALANCE",
  "message": "Saldo insuficiente para FPV_BTG_PACTUAL_RECAUDADORA",
  "timestamp": "2026-03-16T12:00:00Z"
}
```

### Documentacion interactiva

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

---

## Modelo de Datos (DynamoDB)

Se utilizan 4 tablas DynamoDB con billing on-demand (sin capacidad provisionada):

### Tabla `{env}-clients`

| Atributo | Tipo | Clave |
|---|---|---|
| `id` | String | Partition Key |
| `name` | String | - |
| `email` | String | - |
| `phone` | String | - |
| `balance` | Number | - |
| `notificationPreference` | String | - |

### Tabla `{env}-funds`

| Atributo | Tipo | Clave |
|---|---|---|
| `id` | String | Partition Key |
| `name` | String | - |
| `minimumAmount` | Number | - |
| `category` | String | - |

### Tabla `{env}-subscriptions`

| Atributo | Tipo | Clave |
|---|---|---|
| `clientId` | String | Partition Key |
| `fundId` | String | Sort Key |
| `fundName` | String | - |
| `amount` | Number | - |
| `subscribedAt` | String (ISO-8601) | - |

### Tabla `{env}-transactions`

| Atributo | Tipo | Clave |
|---|---|---|
| `clientId` | String | Partition Key |
| `transactionId` | String | Sort Key |
| `fundId` | String | - |
| `fundName` | String | - |
| `type` | String | - |
| `amount` | Number | - |
| `timestamp` | String (ISO-8601) | - |

### Datos iniciales (seed)

El script `infrastructure/scripts/seed-funds.sh` pobla el catalogo de fondos:

| ID | Nombre | Monto Minimo (COP) | Categoria |
|---|---|---|---|
| 1 | FPV_BTG_PACTUAL_RECAUDADORA | $75.000 | FPV |
| 2 | FPV_BTG_PACTUAL_ECOPETROL | $125.000 | FPV |
| 3 | DEUDAPRIVADA | $50.000 | FIC |
| 4 | FDO-ACCIONES | $250.000 | FIC |
| 5 | FPV_BTG_PACTUAL_DINAMICA | $100.000 | FPV |

Ademas crea un cliente de prueba con saldo de COP $500.000.

---

## Componente SQL

La segunda parte de la prueba tecnica se encuentra en el directorio `sql/`:

### Esquema (`sql/init.sql`)

Define 6 tablas en PostgreSQL:

- **Cliente**: clientes del sistema (id, nombre, apellidos, ciudad)
- **Sucursal**: sucursales fisicas (id, nombre, ciudad)
- **Producto**: productos financieros (id, nombre, tipoProducto)
- **Inscripcion**: relacion N:N entre productos y clientes
- **Disponibilidad**: relacion N:N entre sucursales y productos
- **Visitan**: registro de visitas de clientes a sucursales

Incluye datos de prueba con 6 clientes, 4 sucursales, 5 productos y multiples relaciones entre ellos, con casos de prueba documentados para validar la consulta.

### Consulta (`sql/consulta.sql`)

**Problema**: Obtener los nombres de los clientes que tienen inscrito algun producto disponible **solo** en las sucursales que visitan. Es decir, para al menos uno de sus productos inscritos, todas las sucursales donde esta disponible ese producto son sucursales que el cliente visita.

**Solucion**: Usa `NOT EXISTS` con subconsulta correlacionada:

```sql
SELECT DISTINCT c.nombre
FROM Cliente c
JOIN Inscripcion i ON c.id = i.idCliente
WHERE NOT EXISTS (
    SELECT 1
    FROM Disponibilidad d
    WHERE d.idProducto = i.idProducto
      AND d.idSucursal NOT IN (
          SELECT v.idSucursal
          FROM Visitan v
          WHERE v.idCliente = c.id
      )
);
```

**Resultado esperado**: Carlos, Maria, Laura.

---

## Configuracion y Ejecucion Local

### Prerequisitos

- Java 21
- Docker y Docker Compose
- AWS CLI (para seed de datos y despliegue)
- Cuenta AWS con permisos para DynamoDB, SNS, ECS, ECR, CloudFormation

### Variables de entorno

| Variable | Descripcion | Default |
|---|---|---|
| `AWS_REGION` | Region AWS | `us-east-1` |
| `AWS_ENDPOINT` | Endpoint local (LocalStack) | `null` |
| `AWS_ACCESS_KEY` | Access key | `test` |
| `AWS_SECRET_KEY` | Secret key | `test` |
| `DYNAMODB_TABLE_PREFIX` | Prefijo de tablas DynamoDB | `dev-` |
| `SNS_TOPIC_ARN` | ARN del topico SNS | (ver application.yml) |
| `SPRING_PROFILES_ACTIVE` | Perfil de Spring | `dev` |

### Ejecutar con Gradle

```bash
cd api
./gradlew bootRun
```

### Ejecutar con Docker Compose

```bash
docker compose up --build
```

La API estara disponible en `http://localhost:8080`.

### Ejecutar tests

```bash
cd api

# Tests unitarios e integracion
./gradlew test

# Generar reporte de tests
./gradlew test --info
```

### Seed de datos

```bash
./infrastructure/scripts/seed-funds.sh dev us-east-1
```

---

## Infraestructura AWS

Toda la infraestructura se define como codigo con CloudFormation en `infrastructure/cloudformation/`:

### `template.yaml` - Infraestructura de aplicacion

Despliega condicionalmente segun si se proporciona el parametro `AppImage`:

**Siempre se crean:**
- VPC con 2 subnets publicas y 2 privadas
- Internet Gateway + NAT Gateway
- Repositorio ECR
- 4 tablas DynamoDB
- Topico SNS

**Solo si `AppImage` esta presente:**
- Cluster ECS con Container Insights
- Task Definition (512 CPU, 1024 MB RAM)
- ECS Service (2 tareas deseadas, Fargate)
- Application Load Balancer
- Target Group con health checks
- Roles IAM (Task Execution + Task Role)

### Diagrama de infraestructura

```
                    Internet
                       │
                       ▼
              ┌────────────────┐
              │      ALB       │
              │  (Puerto 80)   │
              └───────┬────────┘
                      │
         ┌────────────┴────────────┐
         ▼                         ▼
  ┌──────────────┐         ┌──────────────┐
  │  ECS Task 1  │         │  ECS Task 2  │
  │  (Fargate)   │         │  (Fargate)   │
  │  Subnet Priv │         │  Subnet Priv │
  └──────┬───────┘         └──────┬───────┘
         │                        │
         ▼                        ▼
  ┌─────────────────────────────────────┐
  │          AWS Services               │
  │  ┌───────────┐  ┌───────────────┐   │
  │  │ DynamoDB   │  │     SNS       │   │
  │  │ (4 tablas) │  │ (Email/SMS)   │   │
  │  └───────────┘  └───────────────┘   │
  └─────────────────────────────────────┘
```

### Scripts de despliegue

```bash
# Desplegar infraestructura
./infrastructure/scripts/deploy.sh dev us-east-1

# Destruir infraestructura
./infrastructure/scripts/destroy.sh dev us-east-1
```

---

## Pipeline CI/CD

Definido en `infrastructure/cloudformation/pipeline.yaml`:

```
GitHub (main) ──► CodePipeline ──► CodeBuild ──► CloudFormation Deploy
                      │                │                  │
                      │           Build Docker       Create/Execute
                      │           Push to ECR        Change Set
                      │                │                  │
                      ▼                ▼                  ▼
                  S3 Artifacts    ECR Image         ECS Service Update
                                 (commit SHA)      (rolling deploy)
```

### Flujo detallado

1. **Source**: Push a branch `main` en GitHub (via CodeStar Connection)
2. **Build**: CodeBuild ejecuta `buildspec.yml`:
   - Login a ECR
   - Build de imagen Docker (multi-stage, `linux/amd64`)
   - Tag con SHA del commit
   - Push a ECR
3. **Deploy**: CloudFormation aplica change set:
   - Actualiza Task Definition con nueva imagen
   - ECS Service realiza rolling update
   - ALB health checks validan las nuevas tareas

---

## Testing

### Estrategia de testing

El proyecto implementa una piramide de tests completa:

```
        ┌───────────┐
        │   E2E     │  4 test classes
        │  Tests    │  (SpringBootTest + TestRestTemplate)
        ├───────────┤
        │Integration│  2 test classes
        │  Tests    │  (Controller tests con mocks)
        ├───────────┤
        │   Unit    │  8 test classes
        │  Tests    │  (UseCases + Adapters con Mockito)
        └───────────┘
```

### Tests unitarios

- **Use Cases** (3 clases): Validan toda la logica de negocio incluyendo flujos exitosos y todos los escenarios de error (cliente no encontrado, fondo no encontrado, saldo insuficiente, ya suscrito, suscripcion no encontrada).
- **DynamoDB Adapters** (4 clases): Validan el mapeo entre entidades de dominio y entities de DynamoDB.
- **SNS Adapter** (1 clase): Valida el envio de notificaciones con el formato correcto.

### Tests de integracion

- **FundControllerTest**: Valida el comportamiento del controller de fondos (suscripcion, cancelacion, codigos HTTP, formato de errores).
- **TransactionControllerTest**: Valida el controller de transacciones (paginacion, parametros query).

### Tests E2E

- **FullFlowE2ETest**: Flujo completo de 6 pasos — suscripcion, consulta historial, cancelacion, consulta actualizada.
- **SubscribeFundE2ETest**: Flujo aislado de suscripcion.
- **CancelSubscriptionE2ETest**: Flujo aislado de cancelacion.
- **TransactionHistoryE2ETest**: Flujo aislado de consulta con paginacion.

### Ejecutar tests

```bash
cd api
./gradlew test
```

---

## Decisiones de Diseno

### Arquitectura Hexagonal

Se eligio arquitectura hexagonal para mantener el dominio completamente aislado de frameworks y servicios externos. Esto permite:
- Cambiar DynamoDB por otro motor de persistencia sin tocar la logica de negocio.
- Cambiar SNS por otro servicio de notificaciones sin afectar los casos de uso.
- Testear la logica de dominio sin necesidad de levantar infraestructura.

### Java Records para modelos

Todos los modelos de dominio y DTOs son records inmutables. Esto garantiza thread-safety y evita efectos secundarios en la manipulacion de datos.

### DynamoDB como base de datos

Se eligio DynamoDB por:
- **Serverless**: No requiere gestion de servidores ni capacidad.
- **On-demand billing**: Costo proporcional al uso real.
- **Integracion nativa con AWS**: IAM roles, CloudFormation, sin drivers externos.
- **Performance predecible**: Latencia de un solo digito en milisegundos.

### Desnormalizacion de `fundName`

El nombre del fondo se almacena tanto en subscriptions como en transactions para evitar joins (que DynamoDB no soporta) y optimizar las lecturas del historial de transacciones.

### ECS Fargate sobre EC2

Se eligio Fargate para eliminar la gestion de instancias EC2, con escalamiento automatico y pago por uso de recursos (vCPU + memoria).

### Multi-stage Docker build

Se usa un build en dos etapas (build con Gradle + runtime con JRE Alpine) para minimizar el tamano de la imagen final y reducir la superficie de ataque.

### Command Pattern en Use Cases

Los casos de uso reciben objetos `Command` inmutables en lugar de parametros individuales, lo que facilita la extension y validacion de inputs.

---

## Estructura del Proyecto

```
think-us/
├── api/                          # Aplicacion Spring Boot
│   ├── build.gradle              # Configuracion Gradle
│   ├── buildspec.yml             # Spec de build para CodeBuild
│   ├── Dockerfile                # Docker multi-stage
│   └── src/
│       ├── main/java/com/btg/funds/
│       │   ├── adapter/          # Adaptadores (in/out)
│       │   ├── configuration/    # Configuracion Spring/AWS
│       │   └── domain/           # Logica de negocio pura
│       └── test/                 # Tests (unit, integration, e2e)
├── infrastructure/
│   ├── cloudformation/
│   │   ├── pipeline.yaml         # Pipeline CI/CD
│   │   └── template.yaml         # Infraestructura AWS
│   └── scripts/
│       ├── deploy.sh             # Script de despliegue
│       ├── destroy.sh            # Script de destruccion
│       └── seed-funds.sh         # Seed de datos iniciales
├── sql/
│   ├── init.sql                  # Schema PostgreSQL + datos de prueba
│   └── consulta.sql              # Consulta SQL (Parte 2)
├── docker-compose.yml            # Entorno local con Docker
└── README.md                     # Este archivo
```
