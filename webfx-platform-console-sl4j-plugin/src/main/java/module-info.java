// File managed by WebFX (DO NOT EDIT MANUALLY)

/**
 * SL4J implementation of the Console API.
 */
module webfx.platform.console.sl4j.plugin {

    // Direct dependencies modules
    requires org.slf4j;
    requires webfx.platform.console;

    // Exported packages
    exports dev.webfx.platform.console.spi.impl.sl4j;

    // Provided services
    provides dev.webfx.platform.console.spi.ConsoleProvider with dev.webfx.platform.console.spi.impl.sl4j.Sl4jConsoleProvider;

}