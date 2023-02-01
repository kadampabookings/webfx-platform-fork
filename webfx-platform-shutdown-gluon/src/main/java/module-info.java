// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.shutdown.gluon {

    // Direct dependencies modules
    requires com.gluonhq.attach.lifecycle;
    requires webfx.platform.console;
    requires webfx.platform.shutdown;
    requires webfx.platform.uischeduler;

    // Exported packages
    exports dev.webfx.platform.shutdown.spi.impl.gluon;

    // Provided services
    provides dev.webfx.platform.shutdown.spi.ShutdownProvider with dev.webfx.platform.shutdown.spi.impl.gluon.GluonShutdownProvider;

}