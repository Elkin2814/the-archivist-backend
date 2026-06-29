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

Antes de ejecutar este proyecto de forma local, asegúrate de tener instalado:
* JDK 17 o superior.
* Servidor PostgreSQL corriendo en el puerto `5432`.
* Una base de datos llamada `the_archivist` con un schema del mismo nombre (`the_archivist`).

---

## Configuración del Entorno Local

Este proyecto utiliza variables de entorno para proteger las credenciales y llaves de acceso. 


