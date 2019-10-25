package net.zero9178.mbed.editor

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType
import com.jetbrains.jsonSchema.impl.JsonSchemaVersion

class MbedAppSchemaProvider : JsonSchemaFileProvider {
    override fun getName() = "mbed_app.json Schema"

    override fun isAvailable(file: VirtualFile) = file.name == "mbed_app.json"

    override fun getSchemaFile(): VirtualFile? =
        JsonSchemaProviderFactory.getResourceFile(
            MbedAppLibSchemaProviderFactory::class.java,
            "/schemas/schema_app.json"
        )

    override fun getSchemaType() = SchemaType.embeddedSchema

    override fun getSchemaVersion() = JsonSchemaVersion.SCHEMA_6
}

class MbedLibSchemaProvider : JsonSchemaFileProvider {
    override fun getName() = "mbed_lib.json Schema"

    override fun isAvailable(file: VirtualFile) = file.name == "mbed_lib.json"

    override fun getSchemaFile(): VirtualFile? =
        JsonSchemaProviderFactory.getResourceFile(
            MbedAppLibSchemaProviderFactory::class.java,
            "/schemas/schema_lib.json"
        )

    override fun getSchemaType() = SchemaType.embeddedSchema

    override fun getSchemaVersion() = JsonSchemaVersion.SCHEMA_6
}