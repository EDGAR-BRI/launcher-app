package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

data class AppFolder(
    val id: String,
    val name: String,
    val packageNames: List<String> = emptyList(),
    val showInHome: Boolean = true,
    val showInDrawer: Boolean = true
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        val array = JSONArray()
        packageNames.forEach { array.put(it) }
        json.put("packageNames", array)
        json.put("showInHome", showInHome)
        json.put("showInDrawer", showInDrawer)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): AppFolder {
            val id = json.optString("id", "")
            val name = json.optString("name", "Carpeta")
            val array = json.optJSONArray("packageNames")
            val packages = mutableListOf<String>()
            if (array != null) {
                for (i in 0 until array.length()) {
                    packages.add(array.getString(i))
                }
            }
            val showInHome = json.optBoolean("showInHome", true)
            val showInDrawer = json.optBoolean("showInDrawer", true)
            return AppFolder(
                id = id,
                name = name,
                packageNames = packages,
                showInHome = showInHome,
                showInDrawer = showInDrawer
            )
        }
    }
}
