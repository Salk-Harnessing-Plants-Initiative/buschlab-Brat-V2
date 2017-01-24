import at.ac.oeaw.gmi.brat.gui.fx.log.LogQueue;
import at.ac.oeaw.gmi.brat.segmentation.dispatch.BratDispatcher;
import at.ac.oeaw.gmi.brat.utility.DefaultPreferences;
import ij.plugin.PlugIn;

import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static java.awt.GraphicsEnvironment.isHeadless;
import static javafx.application.Application.launch;


//NOTE: this is Brat_V2-Marco


public class Brat_V2 implements PlugIn {
    private final static Preferences prefs_simple = Preferences.userRoot().node("at/ac/oeaw/gmi/bratv2");
    private final static Preferences prefs_expert = prefs_simple.node("expert");


    public static void main(String[] args) {
        if (prefs_expert.get("LABEL_FONT", null) == null) {
            try {
                DefaultPreferences.create();
                System.out.println("Default preferences created.");
            } catch (BackingStoreException e) {
                System.out.println("Error creating preferences backing store.");
            }
        }

        BratDispatcher dispatcher = new BratDispatcher();
        String hdls = System.getenv("BRAT_RUNHEADLESS");
        if(hdls == null){
            hdls = "false";
        }
        if (isHeadless() || Objects.equals(hdls.toLowerCase(), "true")) {
            dispatcher.initLogger(null);
            dispatcher.runHeadless();
        } else {
            LogQueue logQueue = new LogQueue(1_000_000);
            dispatcher.initLogger(logQueue);
            launch(at.ac.oeaw.gmi.brat.gui.fx.BratFxApp.class);
        }
    }


    @Override
    public void run(String arg) {
        main(null);
    }


}
