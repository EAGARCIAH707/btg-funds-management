# BTG Funds API

API REST para gestión de suscripciones a fondos de inversión.

## Stack técnico

- Java 21, Spring Boot 3.4.3, Gradle
- AWS DynamoDB (persistence), AWS SNS (notifications)
- JUnit 5, Mockito, AssertJ (testing)

## Arquitectura hexagonal

```
com.btg.funds/
├── domain/              ← Centro, sin dependencias externas
│   ├── model/           ← Entidades y value objects del dominio
│   ├── model/enums/     ← Enums del dominio
│   ├── exception/       ← Excepciones de negocio (extienden DomainException)
│   ├── port/in/         ← Interfaces de entrada (contratos de use cases)
│   ├── port/out/        ← Interfaces de salida (repositories, notification)
│   └── usecase/         ← Implementaciones de los use cases
├── adapter/             ← Capa externa, implementa/consume ports
│   ├── in/rest/         ← Controllers REST (adapters de entrada)
│   │   └── model/dto/  ← DTOs de request/response
│   └── out/             ← Adapters de salida
│       ├── dynamo/      ← Implementaciones DynamoDB de repositories
│       │   └── model/entity/ ← Entidades de persistencia
│       └── sns/         ← Implementación SNS de NotificationPort
└── configuration/       ← Configuración Spring (beans, AWS config)
```

### Regla de dependencia

Las dependencias siempre apuntan hacia el centro:
- `domain` NO importa nada de `adapter` ni `configuration` ni frameworks (excepto java.*)
- `adapter` y `configuration` dependen de `domain`
- Nunca introducir anotaciones de Spring, AWS SDK u otros frameworks en `domain`

## Principios de desarrollo

### Código limpio
- Nombres descriptivos e intencionados (clases, métodos, variables)
- Métodos cortos con una sola responsabilidad
- Sin comentarios obvios; el código debe ser autoexplicativo
- Sin código muerto, imports sin usar ni variables ignoradas
- Preferir inmutabilidad: usar records para modelos y DTOs

### SOLID
- **S**: Cada clase tiene una única razón de cambio
- **O**: Extender comportamiento sin modificar código existente (usar interfaces/ports)
- **L**: Las implementaciones deben ser sustituibles por sus interfaces
- **I**: Interfaces pequeñas y específicas por caso de uso (un port in por use case)
- **D**: El dominio depende de abstracciones (ports), nunca de implementaciones concretas

### Patrones aplicados
- **Ports & Adapters**: separación entre dominio y detalles técnicos
- **Command pattern**: los use cases reciben Commands como input
- **Factory method**: métodos estáticos `from()` / `of()` en DTOs y responses
- Aplicar patrones adicionales solo cuando resuelvan un problema concreto, no por anticipación

## Convenciones de código

- Los use cases implementan la interfaz de `domain.port.in` y se nombran `*UseCaseImpl`
- Las interfaces de port in se nombran `*UseCase`
- Los adapters de salida se nombran `*Adapter` (ej: `ClientDynamoAdapter`)
- Las entidades de persistencia se nombran `*Entity`
- DTOs de entrada: `*Request`, de salida: `*Response`
- Excepciones de dominio extienden `DomainException`
- Inyección de dependencias mediante constructor (sin `@Autowired` en campos)
- Beans configurados explícitamente en `BeanConfig`, no con `@Service`/`@Component` en dominio

## Comandos

```bash
./gradlew build          # Compilar y ejecutar tests
./gradlew test           # Solo tests
./gradlew bootRun        # Ejecutar la aplicación
```
