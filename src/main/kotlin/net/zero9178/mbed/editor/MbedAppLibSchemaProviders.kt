package net.zero9178.mbed.editor

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType
import com.jetbrains.jsonSchema.impl.JsonSchemaVersion
import net.zero9178.mbed.state.MbedProjectState

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

class MbedProfileSchemaProvider(private val myProject: Project) : JsonSchemaFileProvider {
    override fun getName() = "Mbed profiles Schema"

    override fun isAvailable(file: VirtualFile) = MbedProjectState.getInstance(myProject).additionalProfiles.any {
        file.path == it
    }

    override fun getSchemaFile(): VirtualFile? =
        JsonSchemaProviderFactory.getResourceFile(
            MbedAppLibSchemaProviderFactory::class.java,
            "/schemas/schema_profile.json"
        )

    override fun getSchemaType() = SchemaType.embeddedSchema

    override fun getSchemaVersion() = JsonSchemaVersion.SCHEMA_6
}