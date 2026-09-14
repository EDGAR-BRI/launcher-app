package com.example.data.model

enum class CustomWidgetId(val title: String, val description: String) {
    CLOCK_DATE("Reloj y Fecha", "Hora digital y fecha completa en tipografía minimalista"),
    BATTERY("Batería y Estado", "Porcentaje de batería y estado de carga"),
    DAILY_INTENTION("Intención / Nota Diaria", "Frase de enfoque personal editable para mantener la concentración"),
    MINDFUL_TIMER("Pausa Consciente / Temporizador", "Temporizador rápido para descansos de pantalla o respiración"),
    QUICK_SEARCH("Barra de Búsqueda", "Acceso rápido a búsqueda web o aplicaciones"),
    QUICK_TOOLS("Herramientas Rápidas", "Accesos directos a linterna, sonido y ajustes del sistema")
}
