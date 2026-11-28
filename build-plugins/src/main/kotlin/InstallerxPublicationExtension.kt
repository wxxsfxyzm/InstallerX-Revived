import org.gradle.api.provider.Property

/** Per-module metadata for modules that apply `installerx.publication`. */
interface InstallerxPublicationExtension {
    /** Short description written to the published POM. */
    val description: Property<String>
}
