# Política de datos de The Archivist

Los datos se separan en tres capas según cuánto deben vivir.

| Capa | Tablas | Vida | Para qué |
|---|---|---|---|
| 1. Referencia | `maps`, `playlists`, `seasons`, `csr_designations` (+ los 16 líderes fijos en código: `LeaderCatalog`) | Permanente. Se descarga de la Metadata de la Halo API al arrancar si falta o tiene más de 30 días | Nombres legibles |
| 2. Caché de jugadores | `players`, `player_matches` | Temporal. Se borra a quien nadie consulta hace 30 días (`archivist.retention.player-cache-days`, tarea diaria 04:30) | Mostrar historial y resumen de un gamertag |
| 3. Análisis | `matches`, `match_participants` | Permanente. **Sin gamertag**: solo partida, índice de jugador, líder, equipo, resultado y rango | Winrates por líder, matchups, meta, entrenamiento de modelos |

Reglas:
- Cada consulta de un jugador alimenta la capa 3 de forma idempotente (por `match_id` + `player_index`).
- Limpiar la capa 2 no toca la capa 3.
- Si Microsoft pide eliminar los datos guardados, se vacían las capas 2 y 3 (`TRUNCATE the_archivist.match_participants, the_archivist.matches, the_archivist.players CASCADE;`).
- Pendiente: ingerir Match Result para llenar `match_participants` con **todos** los jugadores de cada partida (hoy solo entra el jugador consultado); eso habilita los matchups.
