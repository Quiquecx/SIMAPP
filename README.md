SIMA App - Control de Procesos en Tiempo Real 
SIMA App es una solución móvil avanzada diseñada para la optimización de la gestión operativa y supervisión de procesos de entrada (Incoming). La aplicación permite el seguimiento preciso del tiempo invertido en actividades industriales, eliminando las estimaciones manuales y sustituyéndolas por datos exactos y accionables.

Propuesta de Valor
SIMA App transforma la supervisión operativa mediante tres pilares:

Precisión Operativa: Cronometraje exacto vinculado a actividades por ID, Material y Proveedor.

Visibilidad 360°: Dashboard dinámico con KPIs en tiempo real que muestran el pulso de la operación al instante.

Ubicuidad: Sincronización instantánea entre múltiples dispositivos mediante una arquitectura en la nube.

Características Principales:
Dashboard Inteligente

KPIs en Vivo: Visualización inmediata de actividades totales, en curso y porcentaje de progreso promedio.

Búsqueda Reactiva: Filtrado instantáneo por ID de actividad (CPM), Proveedor o Material para auditorías rápidas.

Ordenamiento Prioritario: Las actividades con cronómetros activos se posicionan automáticamente en la parte superior.

Sistema de Cronometraje "Active-Track"

Contadores en Tiempo Real: Visualización de segundos transcurridos directamente en la lista principal y en la pantalla de detalles.

Banner Flotante (Mini Player): Recordatorio persistente de la actividad en curso que acompaña al usuario mientras navega por la app.

Persistencia en la Nube: Si un dispositivo se apaga, el tiempo sigue contabilizándose gracias a la arquitectura basada en estampas de tiempo (ServerTimestamp).

Diseño Premium y Adaptable

Interfaz Adaptativa: Experiencia optimizada para Smartphones y Tablets, aprovechando el espacio en pantallas grandes.

Feedback Visual: Código de colores (Verde/Rojo) para estados de finalización y progreso.

Stack Tecnológico
Lenguaje: Kotlin

UI Framework: Jetpack Compose (Declarative UI)

Arquitectura: MVVM (Model-View-ViewModel) + Clean Architecture (Domain/Entity layers)

Inyección de Dependencias: Hilt (Dagger)

Base de Datos y Backend: Firebase Firestore (NoSQL)

Reactividad: Kotlin Coroutines & Flow (StateFlow, combine, collectAsStateWithLifecycle)

Navegación: Compose Navigation con paso de argumentos complejos.

Estructura del Proyecto
Plaintext
com.quiquecx.simaapp
├── data          # Repositorios y fuentes de datos
├── domain        # Entidades de negocio y casos de uso
├── di            # Módulos de Inyección de Dependencias (Hilt)
└── view          # UI Layers (Screens, ViewModels, Components)
    ├── dashboard # Pantalla principal y KPIs
    ├── details   # Gestión de cronómetros y detalles
    └── create    # Registro de nuevas actividades
