/*
 * Copyright 2025 Kyant (https://github.com/Kyant0/AndroidLiquidGlass)
 * Licensed under the Apache License, Version 2.0. See THIRD_PARTY_NOTICES.md.
 *
 * Vendored into expo-android-glass-view. Changes from upstream: package relocated
 * from com.kyant.backdrop, Kotlin Multiplatform expect/actual merged into Android-only
 * code, dependency on io.github.kyant0:shapes removed, Kotlin 2.1 compatible syntax.
 */
package expo.modules.androidglassview.backdrop


sealed interface RuntimeShaderCache {

    fun obtainRuntimeShader(key: String, string: String): RuntimeShader
}

internal class RuntimeShaderCacheImpl : RuntimeShaderCache {

    private val runtimeShaders = mutableMapOf<String, RuntimeShader>()

    override fun obtainRuntimeShader(key: String, string: String): RuntimeShader {
        // Fast-path: comparte compilación AGSL entre nodos para no pagar
        // 5-20ms por cada pill/diálogo. Misma apariencia, 0 recompilaciones.
        GlobalRuntimeShaderCache.obtain(key)?.let { return it }
        val created = runtimeShaders.getOrPut(key) { RuntimeShader(string) }
        GlobalRuntimeShaderCache.put(key, created)
        return created
    }

    fun clear() {
        runtimeShaders.clear()
    }
}

/**
 * Caché global de shaders compilados. Los RuntimeShader son caros de compilar
 * pero el código AGSL por key es estable ("Refraction", highlights...):
 * reutilizarlos entre nodos no cambia el resultado visual
 * (los uniforms se setean por instancia de uso).
 */
internal object GlobalRuntimeShaderCache {
    private val lock = Any()
    private val cache = LinkedHashMap<String, RuntimeShader>(16, 0.75f, true)

    fun obtain(key: String): RuntimeShader? = synchronized(lock) {
        cache[key]
    }

    fun put(key: String, shader: RuntimeShader) = synchronized(lock) {
        // Acota a ~12 shaders distintos para no retener memoria.
        if (cache.size >= 12 && !cache.containsKey(key)) {
            val eldest = cache.keys.firstOrNull()
            if (eldest != null) cache.remove(eldest)
        }
        if (!cache.containsKey(key)) {
            cache[key] = shader
        }
    }
}
