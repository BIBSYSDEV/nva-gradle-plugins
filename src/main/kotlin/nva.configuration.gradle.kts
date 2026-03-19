import no.unit.nva.gradle.NvaConventionsExtension

// Create the shared extension if it doesn't exist
if (extensions.findByName("nva") == null) {
    extensions.create<NvaConventionsExtension>("nva")
}
