// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.arch.java {

    // Direct dependencies modules
    requires webfx.platform.arch;

    // Exported packages
    exports dev.webfx.platform.arch.spi.impl.java;

    // Provided services
    provides dev.webfx.platform.arch.spi.ArchProvider with dev.webfx.platform.arch.spi.impl.java.JavaArchProvider;

}