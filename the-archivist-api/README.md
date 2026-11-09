# The Archivist API 

**The Archivist** es el backend de una aplicación web diseñada para los jugadores de **Halo Wars 2**. Permite a los usuarios consultar su información general de perfil, revisar historiales de partidas y analizar estadísticas detalladas de combate (líderes más usados, porcentaje de victorias, unidades destruidas, etc.).

---

## Tecnologías Utilizadas

* **Java 17 / 21**
* **Spring Boot 4.x** (Spring Data JPA, Spring Web)
* **PostgreSQL** (Base de datos relacional)
* **Hibernate 7.x** (ORM)
* **Maven** (Gestor de dependencias)

---

## Requisitos Previos

* JDK 17 o superior.
* Servidor PostgreSQL corriendo en el puerto `5432`.
* Una base de datos llamada `the_archivist` con un schema del mismo nombre (`the_archivist`).

---

## Configuración del Entorno Local

Este proyecto utiliza variables de entorno para proteger las credenciales y llaves de acceso. 

## INFORMACIÓN TÉCINA Y CONCEPTUALDEL PROYECTO

# THE ARCHIVIST

## Especificación técnica, arquitectura y alcance del MVP

---

# 1. Visión general

**The Archivist** es una plataforma web de estadísticas y análisis competitivo para **Halo Wars 2**.

La plataforma utilizará la API oficial de Halo Wars 2 como fuente externa de información, pero **no dependerá de ella para atender las consultas de los usuarios**.

El objetivo es construir una capa propia de:

* almacenamiento histórico;
* normalización de datos;
* estadísticas;
* análisis de jugadores;
* análisis de líderes;
* análisis de enfrentamientos;
* evolución temporal;
* rankings;
* meta competitivo.

La API de Halo será considerada una **fuente de datos externa**, mientras que PostgreSQL será la fuente de datos utilizada por la aplicación para responder las consultas del frontend.

---

# 2. Problema que busca resolver

La información proporcionada por la API de Halo Wars 2 es muy valiosa, pero está orientada principalmente a entregar datos técnicos.

The Archivist busca convertir esos datos en una experiencia de análisis mucho más útil para el jugador.

Por ejemplo, en lugar de mostrar solamente:

```text
LeaderId = 7
PlayerMatchOutcome = 1
CSR = ...
```

The Archivist debe poder mostrar:

```text
Forge
Victoria
Platinum III
+15 CSR
23:54
Bridges
```

Y posteriormente generar información de mayor nivel:

```text
Tu líder más utilizado:
Forge

Win Rate:
58.4%

Mejor matchup:
Forge vs Atriox → 63.2%

Peor matchup:
Forge vs Decimus → 42.7%
```

---

# 3. Objetivos principales

La plataforma tendrá dos grandes áreas funcionales.

## A. Player Analytics

Análisis individual de un jugador mediante su Gamertag.

Debe permitir consultar:

* historial de partidas;
* líderes utilizados;
* victorias y derrotas;
* win rate;
* mapas;
* modos de juego;
* duración de partidas;
* CSR;
* evolución de CSR;
* MMR;
* evolución de MMR;
* estadísticas por líder;
* estadísticas por rango;
* estadísticas por mapa;
* estadísticas contra otros líderes.

---

## B. Global Analytics

Análisis general de la comunidad.

Debe permitir posteriormente:

* líderes más utilizados;
* líderes con mayor win rate;
* líderes con menor win rate;
* pick rate;
* win rate;
* estadísticas por rango;
* estadísticas por mapa;
* estadísticas por modo;
* matchups;
* counters;
* evolución del meta;
* estadísticas históricas.

---

# 4. API externa de Halo Wars 2

La API proporciona información de partidas asociadas a un Gamertag.

Endpoint conceptual:

```http
GET /players/{gamertag}/matches
```

Puede recibir parámetros como:

```text
player
matchType
start
count
```

`count` tiene un máximo de 25 resultados por petición.

La respuesta contiene una colección de partidas recientes.

Ejemplo conceptual:

```json
{
    "Start": 0,
    "Count": 25,
    "ResultCount": 25,
    "Results": [
        {
            "MatchId": "...",
            "MatchType": 3,
            "GameMode": 3,
            "SeasonId": "...",
            "PlaylistId": "...",
            "MapId": "...",
            "MatchStartDate": {
                "ISO8601Date": "2026-06-29T16:31:42.615Z"
            },
            "PlayerMatchDuration": "PT23M54.5557132S",
            "PlayerIndex": 6,
            "TeamId": 1,
            "TeamPlayerIndex": 2,
            "LeaderId": 7,
            "PlayerCompletedMatch": true,
            "PlayerMatchOutcome": 1,
            "XPProgress": {},
            "RatingProgress": {}
        }
    ]
}
```

---

# 5. Información disponible por partida

Cada resultado contiene información muy importante.

## Identificación

```text
MatchId
```

Debe utilizarse como identificador único de la partida.

---

## Tipo de partida

```text
MatchType
```

Valores:

```text
Unknown
Campaign
Custom
Matchmaking
```

---

## Modo de juego

```text
GameMode
```

Puede representar:

```text
CampaignSolo
CampaignCooperative
Deathmatch
Domination
Strongholds
Blitz
Firefight
NormalTutorial
BlitzTutorial
TerminusFirefight
```

---

## Temporada

```text
SeasonId
```

Puede ser `null` para partidas que no pertenecen a una playlist ranked.

---

## Playlist

```text
PlaylistId
```

---

## Equipos

La API entrega:

```json
"Teams": {
    "1": {
        "TeamSize": 3
    },
    "2": {
        "TeamSize": 3
    }
}
```

Esto permite conocer la estructura de equipos.

---

## Mapa

```text
MapId
```

Ejemplo:

```text
skirmish\design\MP_Bridges\MP_Bridges
```

El backend posteriormente puede resolver este identificador mediante metadata.

---

## Fecha

```text
MatchStartDate.ISO8601Date
```

Debe convertirse a un tipo temporal adecuado dentro del dominio.

---

## Duración

```text
PlayerMatchDuration
```

Es un ISO 8601 Duration.

Ejemplo:

```text
PT23M54.5557132S
```

Debe transformarse a `Duration` en Java.

---

## Jugador

La respuesta también identifica:

```text
PlayerIndex
TeamId
TeamPlayerIndex
```

---

## Líder

```text
LeaderId
```

Ejemplo:

```text
LeaderId = 7
```

La información del líder no debe duplicarse dentro de la respuesta.

Debe resolverse mediante el módulo de metadata/leaders.

---

## Resultado

```text
PlayerMatchOutcome
```

Valores:

```text
Unknown
Win
Loss
Tie
```

---

## Estado de la partida

```text
PlayerCompletedMatch
```

Permite conocer si el jugador terminó la partida.

---

# 6. XP

La API proporciona:

```text
PreviousTotalXP
GameplayXP
ChallengesXP
UpdatedTotalXP
```

Estos datos pueden almacenarse para permitir posteriormente análisis de progresión.

No son prioritarios para el primer dashboard, pero forman parte de la información histórica disponible.

---

# 7. CSR

La API proporciona dos estados:

```text
PreviousCsr
UpdatedCsr
```

Cada uno puede contener:

```text
Tier
Designation
Raw
PercentToNextTier
MeasurementMatchesRemaining
Rank
```

Esto permite construir posteriormente una gráfica de evolución:

```text
CSR
│
│                 ●
│            ●
│       ●
│  ●
└──────────────────────
        Partidas
```

El MVP debe conservar esta información aunque inicialmente solamente se muestre el valor antes/después de una partida.

---

# 8. MMR

La API proporciona:

```text
PreviousMmr
UpdatedMmr
```

Con:

```text
Rating
Variance
```

Aunque la API indique que algunos campos son para uso interno, deben tratarse de acuerdo con las restricciones y documentación de la API.

Para el MVP puede almacenarse la información necesaria para conservar el historial.

---

# 9. Metadata

La API de Halo también proporciona metadata.

Por ejemplo, el identificador:

```text
LeaderId = 7
```

puede resolverse mediante metadata como:

```text
Id: 7
Name: Forge
```

La metadata también puede proporcionar:

* nombre;
* descripción;
* imagen;
* información visual;
* identificadores;
* mapas;
* playlists;
* temporadas;
* rangos;
* otros elementos del juego.

---

# 10. Imágenes

Cuando la metadata proporciona una URL oficial de imagen, The Archivist debe almacenar únicamente la URL.

Ejemplo conceptual:

```text
image_url =
https://content.halocdn.com/...
```

No es necesario descargar y almacenar las imágenes localmente durante el MVP.

Ventajas:

* menor almacenamiento;
* menor procesamiento;
* utilización del CDN oficial;
* acceso a recursos oficiales;
* menor complejidad.

Debe considerarse, sin embargo, que las URLs externas pueden cambiar o dejar de estar disponibles, por lo que esta decisión puede revisarse posteriormente.

---

# 11. Principio fundamental de consumo de API

El frontend **nunca debe comunicarse directamente con la API de Halo**.

Flujo incorrecto:

```text
Frontend
   ↓
Halo API
```

Esto provocaría que múltiples usuarios puedan generar múltiples peticiones externas.

Ejemplo:

```text
10 usuarios
×
10 consultas
=
100 peticiones a Halo
```

---

# 12. Flujo deseado

The Archivist debe actuar como una capa de cache/persistencia.

```text
                Halo API
                    ↑
                    │
             Sincronización
                    │
                    ↓
               PostgreSQL
                    ↑
                    │
              Spring Boot
                    ↑
                    │
                 Frontend
```

El usuario solamente consulta The Archivist.

---

# 13. Estrategia de sincronización

El sistema debe utilizar sincronización incremental.

No se debe descargar repetidamente todo el historial.

Conceptualmente:

```text
Última partida conocida
        ↓
Consultar nuevas partidas
        ↓
Guardar únicamente nuevas partidas
        ↓
Actualizar lastSync
```

Cada partida debe identificarse mediante su `MatchId`.

Antes de insertar una partida:

```text
¿Existe MatchId?

   Sí → no insertar nuevamente

   No → insertar
```

Esto proporciona idempotencia.

---

# 14. Frecuencia de actualización

Para el MVP puede utilizarse una estrategia de actualización aproximada de:

```text
1 sincronización / minuto
```

Esto no significa necesariamente que cada jugador sea consultado cada minuto.

La estrategia ideal es:

```text
Jugador solicitado
        ↓
¿Existe en BD?
        ↓
¿Está actualizado?
```

Si los datos son recientes:

```text
BD → respuesta
```

Si están desactualizados:

```text
Halo API
   ↓
BD
   ↓
respuesta
```

Posteriormente puede implementarse un Scheduler global.

---

# 15. PostgreSQL como fuente de consulta

Una vez que la información ha sido sincronizada:

```text
GET /players/Elkin/history
```

debe consultar PostgreSQL.

No debe consultar Halo en cada request.

Esto permite:

* escalar horizontalmente;
* soportar múltiples usuarios;
* reducir consumo de API;
* responder rápidamente;
* mantener disponibilidad incluso si la API externa está temporalmente caída.

---

# 16. Modelo conceptual de datos

Para el MVP del perfil de jugador se propone inicialmente:

```text
players
leaders
matches
player_matches
```

Posteriormente:

```text
maps
game_modes
playlists
seasons
ranks
leader_statistics
matchup_statistics
player_statistics
```

---

# 17. Player

Representa al jugador.

Debe contener información propia del jugador.

Conceptualmente:

```java
Player

id
gamerTag
lastSync
createdAt
updatedAt
```

No debería almacenar directamente:

```text
mostPlayedLeader
mostWinRateLeader
```

porque son estadísticas derivadas.

---

# 18. Leader

Representa un líder de Halo Wars 2.

Puede contener:

```text
id
name
description
imageUrl
```

Los líderes son prácticamente datos estáticos.

---

# 19. Match

Representa la partida como un evento global.

No pertenece a un jugador específico.

Puede contener:

```text
matchId
matchType
gameMode
seasonId
playlistId
mapId
startDate
duration
```

---

# 20. PlayerMatch

Representa la participación de un jugador en una partida.

Es uno de los objetos centrales del sistema.

Puede contener:

```text
id

player
match

leader

teamId
playerIndex
teamPlayerIndex

completedMatch
outcome

csrBefore
csrAfter

mmrBefore
mmrAfter

xpBefore
xpAfter
```

Conceptualmente:

```text
Match
 ├── PlayerMatch
 ├── PlayerMatch
 └── PlayerMatch
```

Por lo tanto:

```text
Match ≠ PlayerMatch
```

Una partida puede tener múltiples participantes.

---

# 21. MatchHistory

No necesariamente representa una tabla de PostgreSQL.

Es un modelo utilizado por el caso de uso.

Conceptualmente:

```java
MatchHistory {

    Player player;

    List<PlayerMatch> matches;
}
```

Representa:

> "El historial de este jugador."

---

# 22. Arquitectura Hexagonal

La aplicación utilizará:

```text
DDD + Hexagonal Architecture
```

Cada contexto/módulo seguirá aproximadamente:

```text
player
│
├── application
├── domain
└── infrastructure
```

---

# 23. Domain

El dominio contiene la lógica y modelos de negocio.

No debe depender de:

* Spring;
* JPA;
* Hibernate;
* PostgreSQL;
* HTTP;
* DTOs externos.

Ejemplo:

```text
Player
Match
PlayerMatch
MatchHistory
CsrProgress
MmrProgress
XpProgress
```

---

# 24. Application

Contiene los casos de uso.

Ejemplo:

```text
GetPlayerHistoryUseCase
SyncPlayerHistoryUseCase
GetPlayerProfileUseCase
```

El caso de uso expresa una acción de negocio.

Por ejemplo:

```text
GetPlayerHistory
```

significa:

> Obtener el historial de un jugador.

---

# 25. Port In

Es la interfaz que define lo que la aplicación puede hacer.

Ejemplo conceptual:

```java
GetPlayerHistoryUseCase
```

El Controller utilizará este puerto.

```text
HTTP
 ↓
Controller
 ↓
Port In
 ↓
Application Service
```

El Controller no debe contener lógica de negocio.

---

# 26. Port Out

Define una dependencia que necesita el dominio/application.

Ejemplo:

```java
PlayerHistoryPort
```

Conceptualmente:

```text
Necesito obtener el historial.

No me importa de dónde viene.

```

Puede venir de:

```text
PostgreSQL
Halo API
Redis
otro servicio
```

---

# 27. Adapter

El Adapter implementa un Port.

Por ejemplo:

```text
PlayerHistoryPort
       ↑
       │
HaloHistoryAdapter
```

El adapter sabe cómo comunicarse con una tecnología externa.

Puede ser:

```text
Halo API Adapter
PostgreSQL Adapter
Redis Adapter
```

El dominio no conoce esos detalles.

---

# 28. DTO

Los DTO representan datos de entrada/salida de una frontera.

En este proyecto existirán diferentes DTOs.

Por ejemplo:

```text
HaloMatchHistoryResponse
```

representa la respuesta de la API externa.

Y:

```text
PlayerHistoryResponse
```

representa la respuesta de The Archivist al frontend.

No deben confundirse.

---

# 29. Mapper

El Mapper transforma modelos.

Ejemplo:

```text
Halo API DTO
      ↓
HaloHistoryMapper
      ↓
Domain PlayerMatch
```

Otro mapper puede transformar:

```text
Domain PlayerMatch
      ↓
Persistence Entity
```

Y otro:

```text
Domain
      ↓
API Response DTO
```

El mapper no debe contener lógica de negocio compleja.

Su responsabilidad principal es transformar estructuras.

---

# 30. Persistence Entity

La entidad JPA representa la estructura persistente.

Por ejemplo:

```java
@Entity
@Table(name = "player_matches")
```

Esta clase sí puede utilizar:

```text
@Entity
@Id
@Column
@ManyToOne
```

porque pertenece a Infrastructure.

El dominio no debe utilizar esas anotaciones.

---

# 31. Repository

Debe distinguirse entre:

### Repository Port

Pertenece al lado interno de la arquitectura.

Ejemplo:

```text
PlayerMatchRepositoryPort
```

### JPA Repository

Pertenece a Infrastructure.

Ejemplo:

```text
PlayerMatchJpaRepository
extends JpaRepository
```

El Adapter conecta ambos.

```text
Application / Domain
        ↓
Repository Port
        ↓
Persistence Adapter
        ↓
JPA Repository
        ↓
PostgreSQL
```

---

# 32. Caso de uso principal del MVP

El primer caso de uso importante será:

```text
GetPlayerHistory
```

Endpoint:

```http
GET /players/{gamerTag}/history
```

---

# 33. Flujo del caso de uso

```text
Frontend
   ↓
PlayerController
   ↓
GetPlayerHistoryUseCase
   ↓
PlayerHistoryPort
   ↓
¿Datos disponibles?
   │
   ├── Sí → PostgreSQL
   │
   └── No / desactualizados
              ↓
         Halo API Adapter
              ↓
          Halo API
              ↓
            Mapper
              ↓
        PlayerMatch Domain
              ↓
        Persistence Adapter
              ↓
          PostgreSQL
              ↓
           Response
```

---

# 34. Historial mostrado al usuario

El primer MVP debe mostrar como mínimo:

```text
Gamertag

Última sincronización

Total de partidas

Victorias

Derrotas

Win Rate

Último rango conocido
```

Y una lista de partidas:

```text
Fecha
Mapa
Modo
Líder
Resultado
Duración
CSR antes
CSR después
MMR antes
MMR después
```

---

# 35. Ejemplo de respuesta de API propia

El backend podría devolver algo parecido a:

```json
{
    "gamerTag": "ExamplePlayer",
    "lastSync": "2026-09-13T20:00:00Z",
    "summary": {
        "totalMatches": 100,
        "wins": 56,
        "losses": 44,
        "winRate": 56.0
    },
    "matches": [
        {
            "matchId": "13999c60-4ea7-47cc-a6e2-a99085c4491e",
            "date": "2026-06-29T16:31:42Z",
            "leader": {
                "id": 7,
                "name": "Forge",
                "imageUrl": "https://..."
            },
            "result": "LOSS",
            "map": "Bridges",
            "gameMode": "Deathmatch",
            "durationSeconds": 1434,
            "csr": {
                "before": {},
                "after": {}
            }
        }
    ]
}
```

El frontend no necesita conocer el JSON original de Halo.

---

# 36. Paginación

La API externa permite:

```text
start
count
```

con un máximo de 25 elementos.

The Archivist debe implementar su propia paginación.

Por ejemplo:

```http
GET /players/Example/history?page=0&size=20
```

El frontend no debería depender directamente de `start` y `count` de Halo.

---

# 37. Sincronización incremental

Una sincronización puede funcionar así:

```text
Player = ExamplePlayer

lastKnownMatch =
13999c60-...
```

El sistema solicita partidas recientes a Halo.

Luego compara:

```text
MatchId
```

Si encuentra partidas nuevas:

```text
insert
```

Si encuentra una ya existente:

```text
skip
```

Esto evita duplicados.

---

# 38. Idempotencia

El proceso de sincronización debe ser idempotente.

Ejemplo:

```text
Sincronizar jugador
Sincronizar jugador
Sincronizar jugador
```

No debe generar:

```text
3 registros de la misma partida
```

Debe producir:

```text
1 registro
```

Esto se garantiza mediante restricciones únicas sobre identificadores como:

```text
match_id
```

y/o combinaciones adecuadas como:

```text
player_id + match_id
```

---

# 39. Estadísticas derivadas

No se deben guardar como propiedades directamente dentro de `Player`:

```text
mostPlayedLeader
mostWinRateLeader
```

Estas son estadísticas derivadas.

Pueden calcularse desde `PlayerMatch`.

Por ejemplo:

```text
Player
   ↓
PlayerMatch
   ↓
GROUP BY Leader
   ↓
COUNT
```

Posteriormente, por rendimiento, podrán materializarse en tablas de estadísticas.

---

# 40. Futuras estadísticas de jugador

A partir de `PlayerMatch` se podrán calcular:

### Win Rate

```text
wins / total matches
```

### Líder favorito

```text
leader with highest match count
```

### Win Rate por líder

```text
leader
matches
wins
losses
winRate
```

### Win Rate por mapa

```text
map
matches
wins
winRate
```

### Win Rate por modo

```text
gameMode
matches
wins
winRate
```

### Evolución de CSR

```text
date
csr
```

### Evolución de MMR

```text
date
mmr
```

---

# 41. Futuro módulo de Leaders

Posteriormente existirá:

```text
leader
├── application
├── domain
└── infrastructure
```

Será responsable de información de líderes y posteriormente estadísticas globales.

Ejemplos de casos de uso:

```text
GetAllLeaders
GetMostPlayedLeaders
GetHighestWinRateLeaders
GetLowestWinRateLeaders
GetLeaderStatistics
```

---

# 42. Estadísticas globales

El dashboard global deberá poder mostrar:

```text
Most Played Leader
Highest Win Rate Leader
Lowest Win Rate Leader
```

Inicialmente de forma global.

Posteriormente mediante filtros:

```text
Rank
Map
Game Mode
Playlist
Season
Date Range
```

---

# 43. Estadísticas agregadas

Cuando la cantidad de partidas aumente, no se debe ejecutar continuamente:

```sql
COUNT(*)
GROUP BY leader
```

sobre millones de partidas.

Se podrán crear tablas materializadas/agregadas como:

```text
leader_daily_stats
```

con:

```text
date
leader_id
matches
wins
losses
pick_rate
win_rate
```

Esto permite que el dashboard consulte directamente estadísticas previamente calculadas.

---

# 44. Matchups

Posteriormente:

```text
matchup_statistics
```

Ejemplo:

```text
leader_id
enemy_leader_id
matches
wins
losses
win_rate
```

Esto permitirá:

```text
Forge vs Atriox
Forge vs Cutter
Forge vs Isabel
```

y posteriormente:

```text
Best Counters
Worst Matchups
```

---

# 45. Histórico

The Archivist debe conservar información histórica.

Por ejemplo:

```text
2026-09-01
Forge WR = 54.2%

2026-09-02
Forge WR = 54.8%

2026-09-03
Forge WR = 55.1%
```

Esto permitirá gráficas de evolución.

La información histórica no debería depender de consultar nuevamente Halo.

---

# 46. Dashboard futuro

El dashboard global podría contener:

## KPI

```text
Total Matches
Active Players
Most Played Leader
Highest Win Rate
```

## Leader Performance

Gráficas de:

```text
Pick Rate
Win Rate
```

## Historical Meta

Gráfica:

```text
Win Rate over time
Pick Rate over time
```

## Matchups

Matriz:

```text
        Forge  Atriox  Cutter
Forge     -      55%     62%
Atriox   45%      -      51%
Cutter   38%     49%      -
```

---

# 47. MVP realista

El MVP debe limitarse a:

## Backend

* Spring Boot.
* Java 21.
* PostgreSQL.
* Arquitectura Hexagonal.
* DDD.
* Player module.
* Leader metadata.
* Consumo de API de Halo.
* Persistencia de historial.
* Sincronización.
* Endpoint de historial.
* Paginación.
* Evitar duplicados.

## Frontend

Página:

```text
Player Profile
```

Entrada:

```text
Gamertag
```

Resultado:

```text
Profile Summary
+
Recent Match History
```

---

# 48. Lo que NO debe entrar inicialmente

No implementar todavía:

* autenticación de usuarios;
* sistema social;
* amigos;
* comentarios;
* IA;
* predicciones;
* build orders;
* recomendaciones automáticas;
* tier list;
* sistema avanzado de counters;
* microservicios;
* Kafka;
* Redis;
* Elasticsearch.

La prioridad es construir correctamente la base.

---

# 49. Evolución prevista

La evolución del proyecto sería:

```text
FASE 1
Player History
        ↓
FASE 2
Player Statistics
        ↓
FASE 3
Global Leader Statistics
        ↓
FASE 4
Matchups / Counters
        ↓
FASE 5
Historical Meta
        ↓
FASE 6
Advanced Analytics
```

---

# 50. Principio arquitectónico fundamental

La aplicación debe poder cambiar la fuente de datos sin modificar el dominio.

Actualmente:

```text
Halo API
```

Posteriormente podría existir:

```text
Halo API
+
cache
+
database
+
scheduled synchronization
```

Pero el dominio debe continuar trabajando con conceptos como:

```text
Player
Match
PlayerMatch
Leader
```

y no con conceptos específicos de HTTP o del JSON de Microsoft.

---

# 51. Resultado esperado

El resultado del MVP será una aplicación donde un usuario pueda introducir:

```text
Gamertag
```

y obtener una página de perfil con información histórica de Halo Wars 2.

Ejemplo conceptual:

```text
┌─────────────────────────────────────────────┐
│              THE ARCHIVIST                  │
├─────────────────────────────────────────────┤
│                                             │
│  ExamplePlayer                              │
│                                             │
│  Win Rate       Matches       Leader        │
│  56.0%          100           Forge         │
│                                             │
├─────────────────────────────────────────────┤
│              MATCH HISTORY                  │
├──────────┬────────┬─────────┬──────┬────────┤
│ Date     │ Leader │ Map     │ W/L  │ CSR    │
├──────────┼────────┼─────────┼──────┼────────┤
│ Jun 29   │ Forge  │ Bridges │ Loss │ +/−    │
│ Jun 29   │ Atriox │ Rift    │ Win  │ +/−    │
│ Jun 28   │ Forge  │ Vault   │ Win  │ +/−    │
└──────────┴────────┴─────────┴──────┴────────┘
```

Este MVP no debe intentar resolver todo el análisis competitivo desde el primer día.

Debe construir una **base de datos histórica confiable de partidas y jugadores**, utilizando la API de Halo de forma eficiente.

Esa base será posteriormente utilizada para construir el sistema de estadísticas, líderes, matchups, counters y análisis del meta de The Archivist.

