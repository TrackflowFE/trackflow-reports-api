# Plan de reduccion de complejidad ciclomatica

## Objetivo y alcance

Analisis de `trackflow-reports-api`, revision `b73c833`, realizado el 2026-10-20. Reducir la complejidad ciclomatica del codigo de produccion sin modificar funcionalidades, contratos HTTP/RabbitMQ, esquema ni comportamiento de seguridad. Este documento no autoriza implementar cambios: el usuario solicito revision y plan solamente.

Se leyeron los 60 archivos Java de `src/main/java`. El arbol de trabajo estaba limpio antes y despues del analisis. No se modifico codigo ni configuracion y no se ejecutaron pruebas. El unico archivo creado es este plan.

## Medicion actual

Censo manual de decisiones de fuente, no salida de SonarQube, PMD o JaCoCo. No hay analizador de complejidad configurado en `pom.xml`. La ejecucion de Java/jshell no estuvo permitida en este modo; no se intento eludir esa restriccion.

Para cada metodo o constructor explicito con cuerpo:

`V = 1 + if + ciclos + catch + alternativas case no-default + ternarios + && + ||`.

- Cada alternativa `case` agrega uno; el `switch` no agrega otro punto. No existen etiquetas multialternativa en esta revision.
- Las decisiones dentro de lambdas se atribuyen al metodo envolvente; una lambda no agrega una base independiente.
- Se cuentan los cuerpos explicitos de clases anonimas y tipos anidados, incluidos constructores vacios.
- Se excluyen firmas sin cuerpo, miembros generados de record/enum/Lombok, constructores implicitos, pruebas, `target`, SQL y las ramas internas de bibliotecas.
- No se suman `else`, `return`, `throw`, comparaciones sin control de flujo ni invocaciones Stream/Optional.
- Los inicializadores se inspeccionaron aparte: actualmente contienen cero decisiones contabilizables. Las futuras tablas tambien deben auditarse para no trasladar ramas a inicializadores sin reportarlas.
- Se incluye una segunda variante, `Vc`, que no suma los operadores `&&`/`||`. No se deben mezclar resultados de variantes o herramientas diferentes.

| Metrica | V con cortocircuito | Vc sin operadores booleanos |
| --- | ---: | ---: |
| Archivos Java de produccion | 60 | 60 |
| Metodos / constructores explicitos | 114 / 34 | 114 / 34 |
| Cuerpos contabilizados | 148 | 148 |
| Suma de complejidades | 226 | 213 |
| Media por cuerpo | 1,527 | 1,439 |
| Maximo individual | 8 | 7 |
| Cuerpos con decisiones | 31 | 31 |
| Cuerpos con complejidad mayor que 10 | 0 | 0 |

La suma es un agregado de complejidades de cuerpos independientes, no una unica complejidad del grafo completo de la API. Los numerosos getters y constructores reducen la media; por ello se informa tambien el maximo y el numero de decisiones.

Cuadre: `148 bases + 31 if + 2 ciclos for + 13 case + 19 ternarios + 8 && + 5 || = 226`. Sin operadores booleanos: `148 + 65 = 213`.

Distribucion de V: `1:117; 2:9; 3:9; 4:6; 5:4; 6:2; 7:0; 8:1`.

| Grupo | Archivos | Cuerpos | Suma V | Suma Vc |
| --- | ---: | ---: | ---: | ---: |
| Aplicacion raiz y bootstrap/security | 8 | 12 | 21 | 18 |
| shared/events | 3 | 2 | 2 | 2 |
| reports/domain | 8 | 54 | 79 | 73 |
| reports/application | 17 | 28 | 50 | 49 |
| reports/infrastructure, incluida messaging | 14 | 33 | 41 | 38 |
| reports/api, incluidos DTO | 10 | 19 | 33 | 33 |
| Total | 60 | 148 | 226 | 213 |

## Propuesta cuantificada

Las rutas siguientes son relativas a `src/main/java/com/trackflow/`. Las lineas corresponden a la revision analizada.

| Metodo y ubicacion | V actual | V prevista | Reduccion | Tipo de cambio |
| --- | ---: | ---: | ---: | --- |
| `SecurityConfig.filterChain`, `bootstrap/security/SecurityConfig.java:18` | 3 | 2 | 1 | Consolidar dos decisiones sobre `enabled()` |
| `ConsultarTiemposDeTransito.ejecutar`, `modules/reports/application/ConsultarTiemposDeTransito.java:33` | 5 | 4 | 1 | Una comprobacion de medicion ausente en lugar de dos ternarios |
| `TiemposDeTransitoResponse.formatear`, `modules/reports/api/dto/TiemposDeTransitoResponse.java:61` | 5 | 4 | 1 | Recorrido ordenado con un unico filtro de unidades positivas |
| `ShipmentTransitView.registrar`, `modules/reports/domain/ShipmentTransitView.java:46` | 8 | 3 | 5 | Tabla de acceso a los cinco campos; conservar guard temporal |
| `ShipmentTransitView.get`, `modules/reports/domain/ShipmentTransitView.java:62` | 6 | 1 | 5 | Reutilizar tabla en lugar del segundo switch |
| `HitoDelRecorrido.deTipoDeEvento`, `modules/reports/domain/HitoDelRecorrido.java:17` | 3 | 1 | 2 | Indice inmutable por nombre, no recorrido por consulta |
| `ProyectarVolumenDeEnvios.alRegistrarEvento`, `modules/reports/application/ProyectarVolumenDeEnvios.java:47` | 5 | 3 | 2 | Tabla de operaciones de ingreso/entrega |
| `EnviosRetrasadosResponse.ShipmentResponse.severidad`, `modules/reports/api/dto/EnviosRetrasadosResponse.java:40` | 4 | 1 | 3 | Correspondencia explicita enum a texto HTTP |
| Total | 39 | 19 | 20 | |

Los tres primeros cambios simplifican comprobaciones repetidas. Los cinco restantes representan reglas como datos y eliminan ramas explicitas de los metodos; NO eliminan casos funcionales, decisiones internas de bibliotecas ni obligaciones de prueba. Tampoco son reducciones de complejidad algoritmica o cognitiva demostradas.

| Escenario | Suma V | Reduccion V | Decisiones explicitas | Maximo V | Suma Vc |
| --- | ---: | ---: | ---: | ---: | ---: |
| Actual | 226 | 0 | 78 | 8 | 213 |
| Conservador: primeros tres cambios | 223 | 3 / 1,33 % | 75 | 8 | 210 |
| Ampliado: los ocho cambios | 206 | 20 / 8,85 % | 58 | 6 | 194 |

Objetivo ampliado: misma cantidad de cuerpos explicitos (148), media `206/148 = 1,392`, decisiones explicitas `78 -> 58` (-25,64 %), maximo `8 -> 6` (-25 %). La variante Vc bajaria `213 -> 194`, 19 puntos o 8,92 %.

Estas cifras son proyecciones condicionadas a las estructuras descritas y a no introducir otros cuerpos o decisiones. La cifra final debe volver a medirse sobre la implementacion. `206` es un objetivo concreto y razonable, no un minimo matematico demostrado.

## Pasos de implementacion futura

1. Fijar la misma regla de medicion y conservar el snapshot actual. Si se automatiza, analizar el AST con Java 21 y configurar/documentar switches, operadores booleanos, lambdas, constructores y codigo generado. Excluir pruebas y compilados de la comparacion. Si la herramienta difiere, volver a medir ambos snapshots con ella, no comparar su resultado con 226 como si fueran equivalentes.

2. Ampliar primero las pruebas de caracterizacion de los metodos afectados. Mantener las pruebas existentes; no actualizar expectativas de contratos para hacer pasar un refactor. Cubrir los casos detallados en la seccion de validacion.

3. Aplicar el escenario conservador, sin extraer helpers exclusivamente para reducir una cifra local. En seguridad, registrar primero los matchers publicos, tomar una sola decision `enabled` que configure tanto autorizacion de reportes como JWT/advertencia, y agregar `anyRequest().denyAll()` al final. En tiempos, conservar el retorno temprano cuando no hay entregados y usar una sola rama `m == null` para ambos valores del tramo, manteniendo el fallback independiente de `TOTAL`. En el formateador, usar valores y sufijos ordenados de dias/horas/minutos, un ciclo y un `if (valor > 0)`; conservar fallback y `trim` sin helpers adicionales. Volver a medir: objetivo 223.

4. Sustituir los dos switches de `ShipmentTransitView` por una unica tabla estatica `HitoDelRecorrido -> acceso de lectura/escritura`. Usar referencias a los getters existentes y lambdas de asignacion a los campos existentes. Un record privado para el par de funciones no requiere metodos o constructores explicitos. Mantener todos los campos, anotaciones JPA y getters: no reemplazar la persistencia por un mapa de hitos. Conservar el guard `actual != null && !at.isBefore(actual)` y los retornos booleanos. `get(null)` y `registrar(null, ...)` deben seguir fallando; no agregar validacion de `at` que modifique la semantica actual. No usar `ordinal`. Probar exhaustividad de la tabla contra todas las constantes del enum: la tabla pierde la comprobacion exhaustiva del switch por el compilador.

5. Indexar `HitoDelRecorrido` por `name()` una sola vez. Conservar `Optional.empty()` para un nombre desconocido y para `null`, sin introducir excepciones de `Enum.valueOf`. Elegir un mapa con lookup tolerante a `null`, por ejemplo un HashMap encapsulado con `Collections.unmodifiableMap`. No usar `Map.of`/`Map.copyOf` directamente para este lookup: pueden rechazar una clave nula. Si la tabla se genera con Stream, reconocer que se delega la iteracion de inicializacion a la biblioteca y auditar esa inicializacion; no reescribir cada consulta como Stream para ocultar el ciclo.

6. Representar las dos operaciones de `ProyectarVolumenDeEnvios` mediante una tabla inmutable de handlers: `RECEIVED_AT_CENTER -> registrarIngreso` y `DELIVERED -> registrarEntrega`. El lookup debe aceptar tipo nulo/desconocido y retornar antes de consultar el repositorio. Mantener `findByTrackingNumber(...).ifPresent(...)`, guardar solo si el handler devuelve aplicado y conservar `@Transactional`. Los handlers no deben introducir decisiones nuevas ni exigir campos historicos opcionales. La construccion de `Ingreso` debe usar exactamente los mismos datos del evento.

7. Reemplazar el switch de severidad HTTP por la tabla explicita `LEVE -> LOW`, `MODERADA -> MODERATE`, `CRITICA -> CRITICAL`, sin cambiar el enum de dominio ni sus umbrales. Conservar rechazo de `null` mediante validacion de biblioteca o lookup equivalente. Probar exhaustividad y valores exactos; no usar `name()` ni indices ordinales. Si la tabla aumenta la complejidad accidental o dificulta mantenimiento, conservar el switch y reportar objetivo alternativo 209 en vez de forzar 206.

8. Medir cada cambio y validar el resultado conjunto. Las nuevas tablas, lambdas, inicializadores y helpers forman parte de la auditoria; no transferir ramas a codigo excluido. Aceptar el objetivo 206 solo si se concreta la reduccion y todas las pruebas de equivalencia pasan. Registrar por separado cualquier desviacion de la proyeccion.

## Limites funcionales

- No eliminar validaciones de claims/sesion ni cambiar HS256, issuer, roles, revocacion, inactividad o condiciones SQL. `SharedSessionValidator.validate` conserva V=5.
- No fusionar los fallbacks independientes de identificador/nombre de ciudad ni exponer direccion antes de `DELIVERED`. `ConsultaEnvioController.consultar` conserva V=4.
- No eliminar el guard de existencia de las tres proyecciones, convertir listeners a asincronos, cambiar limites transaccionales, ACK/retry/DLQ ni crear vistas parciales. El listener de movimientos conserva V=4.
- Tracking conserva el ultimo movimiento; transito y volumen conservan los hitos mas tempranos. Un movimiento con timestamp igual se aplica en tracking, pero se ignora si el mismo hito ya existe en transito/volumen. No unificar estas reglas.
- Conservar fechas inclusivas en America/Bogota, corte exclusivo, errores actuales, mensajes, listas vacias/nulos, orden de tramos y de retrasos, redondeo de agregados y umbrales de retraso. `PeriodoDeConsulta.resolver` conserva V=6 y sera el nuevo maximo.
- No trasladar decisiones a SQL, quitar estados ni modificar contratos con productores para mejorar la metrica. No hay migraciones de datos ni cambios de despliegue requeridos.

## Validacion y aceptacion

1. Seguridad: comparar enabled/disabled, los tres GET de reportes, roles OPERADOR/ADMIN/CLIENTE, tokens ausentes/invalidos, publicos, OPTIONS y denegacion de otras rutas/metodos. Mantener el comportamiento con Bearer invalido en rutas publicas.
2. Tiempos: cero entregados, medicion de tramo ausente, medicion presente con promedio nulo, TOTAL ausente, orden y conteos. Formato: cero, subminuto, 60 s, 24 h, unidades intermedias cero, combinaciones y comportamiento negativo actual; conservar segundos y texto exactos.
3. Tabla de hitos: parametrizar los cinco hitos; comprobar lectura/escritura aislada del campo correcto, primer registro, anterior, igual, posterior, reentregas y null. Verificar persistencia/carga JPA de los mismos campos y columnas, sin cambio de esquema.
4. Tipos y volumen: nombres exactos, desconocidos, diferencias de mayusculas y null; desconocidos no consultan repositorio. Comprobar ausencia de vista, ingreso/entrega, historicos con datos opcionales nulos, reentregas y guardado exclusivamente cuando aplicado.
5. Severidad: comprobar todas las constantes y textos JSON exactos, sin cambiar umbrales de dominio ni truncamiento a horas.
6. Regresion de limites intactos: contratos HTTP y errores, direccion/ciudad de tracking, rollback de las tres proyecciones y fallos que permiten retry. Mantener orden de validaciones y efectos laterales.
7. Tras autorizacion de implementacion, ejecutar `./mvnw.cmd test` y `./mvnw.cmd verify` con JDK 21. Para los cambios en entidades, verificar tambien round-trip y agregados en PostgreSQL compatible. Confirmar que no cambian endpoints, JSON, tablas ni eventos.
8. Recontar produccion completa con la misma regla: objetivo ampliado `N=148`, `suma V=206`, `decisiones=58`, `maximo V=6`, `suma Vc=194`; no aceptar mejoras locales acompanadas de crecimiento global no informado.

Hay 27 pruebas existentes en cinco clases. Los informes Surefire preexistentes muestran cero fallos/errores, pero no se ejecutaron en esta revision de analisis. No existe porcentaje de cobertura de ramas acreditado. Los tests de mensajeria llaman al listener con H2 y Rabbit desactivado; no prueban el broker real ni el SQL nativo de PostgreSQL. Por ello no se puede prometer equivalencia verificada antes de implementar y ejecutar esta validacion.

## Alternativas no priorizadas

Extraer metodos puede reducir el maximo local y aumentar la suma global: un cuerpo de V=5 dividido en V=3 y V=3 suma 6. Guard clauses o menor anidamiento tampoco reducen V si conservan las mismas decisiones. Sustituir todas las ramas por Optional/Stream solamente para bajar la cifra no es un objetivo del plan.

Una extraccion reutilizable del predicado temporal compartido por transito y las dos escrituras de volumen podria bajar adicionalmente `206 -> 205` (9,29 % frente al original): cada uno de esos tres guards pierde un operador booleano, pero se agrega un helper V=2. Quedarian 149 cuerpos y 56 decisiones explicitas. Sin embargo, Vc SUBIRIA `194 -> 195`, se introduce una abstraccion por ahorrar un solo punto y el maximo sigue en 6. No se recomienda como parte del plan principal.

No se ha demostrado un minimo absoluto preservando todos los comportamientos. El plan prioriza la reduccion auditada de fuente y evita confundir una API funcionalmente mas simple con una representacion que tiene menos ramas explicitas.
