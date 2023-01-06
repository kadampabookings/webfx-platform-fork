// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.visibility.gluon {

    // Direct dependencies modules
    requires com.gluonhq.attach.lifecycle;
    requires webfx.platform.visibility;

    // Exported packages
    exports dev.webfx.platform.visibility.spi.impl.gluon;

    // Provided services
    provides dev.webfx.platform.visibility.spi.VisibilityProvider with dev.webfx.platform.visibility.spi.impl.gluon.GluonVisibilityProvider;

}