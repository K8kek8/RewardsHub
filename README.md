# Rewards Hub

App de Android (Kotlin + Jetpack Compose) que envuelve la web de Microsoft Rewards en un WebView con una interfaz simple y soporte para varias cuentas.

## Características

- **Checklist diario por cuenta:** set diario, tarjetas, búsquedas de PC y móviles, check-in y bonos. Se reinicia solo cada día.
- **Atajos:** Buscar (`bing.com`), Tarjetas (`rewards.bing.com`) y Puntos (desglose de puntos).
- **Modo PC / Móvil:** cambia el user agent del WebView y recarga la página.
- **Varias cuentas:** cada cuenta usa un perfil de WebView aislado (cookies y almacenamiento separados), así que no hay que volver a iniciar sesión al cambiar.
- **Privacidad:** todo se guarda localmente en el teléfono. Sin servidores ni cuentas propias.

## Requisitos

- Android Studio reciente
- Android 8.0 (API 26) o superior
- Android System WebView actualizado (los perfiles aislados dependen de su versión)

## Cómo compilar

1. Clona el repositorio y ábrelo en Android Studio.
2. Espera a que Gradle sincronice.
3. Ejecuta la app en un dispositivo o emulador.

## Compilar con GitHub Actions

El repositorio incluye `.github/workflows/build.yml`. En cada push se genera un APK de depuración: entra a la pestaña **Actions**, abre la ejecución y descarga el archivo desde **Artifacts** (`RewardsHub-debug`). También puedes lanzarlo a mano con **Run workflow**.

## Lo que la app NO hace

- No automatiza búsquedas, clics ni tareas. Cada acción la haces tú.
- No lee tus puntos automáticamente (Microsoft no ofrece API pública).
- No incluye "Leer para ganar": esa función solo existe en las apps oficiales de Bing y Edge.

## Aviso

Proyecto personal y no oficial, sin relación con Microsoft. Microsoft Rewards, Bing y Edge son marcas de Microsoft. Las reglas del programa establecen una cuenta por persona; úsala conforme a sus términos.
