package com.example.data.model

enum class GestureType(val title: String) {
    SWIPE_UP("Deslizar hacia arriba"),
    SWIPE_DOWN("Deslizar hacia abajo"),
    SWIPE_LEFT("Deslizar a la izquierda"),
    SWIPE_RIGHT("Deslizar a la derecha"),
    DOUBLE_TAP("Doble toque"),
    LONG_PRESS("Pulsación prolongada")
}

enum class LauncherAction(val title: String) {
    OPEN_DRAWER("Abrir cajón de apps"),
    OPEN_NOTIFICATIONS("Expandir notificaciones"),
    OPEN_SETTINGS("Abrir ajustes del launcher"),
    OPEN_QUICK_SEARCH("Búsqueda rápida"),
    OPEN_CAMERA("Abrir cámara"),
    OPEN_CLOCK("Abrir reloj / alarma"),
    TOGGLE_FLASHLIGHT("Alternar linterna"),
    OPEN_RECENTS("Apps recientes / Multitarea"),
    SWITCH_TO_PREVIOUS_APP("Volver a app anterior"),
    NONE("Sin acción")
}
