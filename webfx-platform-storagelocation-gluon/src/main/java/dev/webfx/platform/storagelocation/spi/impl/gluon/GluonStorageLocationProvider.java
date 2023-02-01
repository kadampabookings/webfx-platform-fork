package dev.webfx.platform.storagelocation.spi.impl.gluon;

import com.gluonhq.attach.storage.StorageService;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.storagelocation.spi.StorageLocationProvider;
import dev.webfx.platform.uischeduler.UiScheduler;

import java.io.File;

/**
 * @author Bruno Salmon
 */
public final class GluonStorageLocationProvider implements StorageLocationProvider {

    private StorageService storageService;

    public GluonStorageLocationProvider() {
        // Note: All Gluon servies must be created in the UI thread, otherwise the application crashes
        UiScheduler.runInUiThread(() -> {
            storageService = StorageService.create().orElse(null);
            if (storageService == null) // It seems the audio service is implemented only for Android, so this happens on other platforms
                Console.log("WARNING [WebFX Platform]: Unable to load Gluon Storage Service");
        });
    }

    @Override
    public String getInternalStorageLocation() {
        if (storageService == null) {
            Console.log("WARNING [WebFX Platform]: Could not get the internal storage location as the Gluon Storage Service is not loaded -> returning null");
            return null;
        }
        File privateStorage = storageService.getPrivateStorage().orElse(null);
        if (privateStorage != null)
            return privateStorage.getAbsolutePath();
        Console.log("WARNING [WebFX Platform]: Could not get the Private Storage from Gluon Storage Service -> returning null");
        return null;
    }

}
