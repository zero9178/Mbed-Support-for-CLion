package net.zero9178.mbed.editor

import com.intellij.openapi.project.Project
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory

class MbedAppLibSchemaProviderFactory : JsonSchemaProviderFactory {
    override fun getProviders(project: Project) = mutableListOf(MbedAppSchemaProvider(), MbedLibSchemaProvider())
}