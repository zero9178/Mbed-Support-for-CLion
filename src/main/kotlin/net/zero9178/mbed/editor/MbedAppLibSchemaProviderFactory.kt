package net.zero9178.mbed.editor

import com.intellij.openapi.project.Project
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory

/**
 * Provides json schemas for mbed_app.json and mbed_lib.json
 */
class MbedAppLibSchemaProviderFactory : JsonSchemaProviderFactory {
    override fun getProviders(project: Project) =
        mutableListOf(MbedAppSchemaProvider(), MbedLibSchemaProvider(), MbedProfileSchemaProvider(project))
}