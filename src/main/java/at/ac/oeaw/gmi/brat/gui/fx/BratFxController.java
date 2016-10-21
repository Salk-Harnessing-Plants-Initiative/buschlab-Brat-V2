package at.ac.oeaw.gmi.brat.gui.fx;

import at.ac.oeaw.gmi.brat.gui.fx.log.LogBox;
import at.ac.oeaw.gmi.brat.gui.fx.log.LogQueue;
import at.ac.oeaw.gmi.brat.segmentation.dispatch.BratDispatcher;
import at.ac.oeaw.gmi.brat.utility.ExceptionLog;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.Enumeration;
import java.util.logging.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


public class BratFxController {
    private final static Logger log = Logger.getLogger(BratFxController.class.getName());
    private final static Preferences prefs_simple = Preferences.userRoot().node("at/ac/oeaw/gmi/bratv2");
    private final static Preferences prefs_expert = prefs_simple.node("expert");
    private final LogQueue logQueue;

    //paneBasic
    @FXML private TextField txtfldBaseDir;
    @FXML private Button btnBaseDirBrowse;
    @FXML private Button btnStart;
    @FXML private TextField txtfldExtension;
    @FXML private CheckBox chkboxFlipHorizontal;
    @FXML private CheckBox chkboxTimeSeries;
    @FXML private CheckBox chkboxDayZero;
    @FXML private Slider sliderNumThreads;
    @FXML private Label lblNumThreads;

    //paneExpert
    @FXML private TextField txtfldExpIdentifier;
    @FXML private TextField txtfldSetIdentifier;
    @FXML private TextField txtfldDayIdentifier;
    @FXML private TextField txtfldPlateIdentifier;
    @FXML private TextField txtfldImageResolution;
    @FXML private TextField txtfldSeedMinSize;
    @FXML private TextField txtfldSeedMaxSize;
    @FXML private TextField txtfldShootMinThickness;
    @FXML private TextField txtfldShootWidthStep;
    @FXML private TextField txtfldShootHeightStep;
    @FXML private TextField txtfldPlantMinThickness;
    @FXML private TextField txtfldPlantMinHeight;
    @FXML private TextField txtfldPlantWidthStep;
    @FXML private TextField txtfldPlantHeightStep;
    @FXML private TextField txtfldPlantTreeMaxLevel;
    @FXML private TextField txtfldPlantTrackback;
    @FXML private TextField txtfldCircleDiameter;
    @FXML private TextField txtfldLabelSize;
    @FXML private TextField txtfldLabelFont;
    @FXML private TextField txtfldNumberSize;
    @FXML private TextField txtfldNumberFont;
    @FXML private ColorPicker clrpckShootRoi;
    @FXML private ColorPicker clrpckRootRoi;
    @FXML private ColorPicker clrpckSkeleton;
    @FXML private ColorPicker clrpckMainRoot;
    @FXML private ColorPicker clrpckStartPoint;
    @FXML private ColorPicker clrpckEndPoint;
    @FXML private ColorPicker clrpckShootCM;
    @FXML private ColorPicker clrpckGeneral;
    @FXML private ChoiceBox<String> choiceboxLogLevel;

    @FXML private LogBox logBox;
    @FXML private Accordion accordionMain;
    @FXML private TitledPane paneLog;

    public BratFxController(LogQueue logQueue){
        this.logQueue=logQueue;
    }

    @FXML
    private void initialize(){
        txtfldBaseDir.setText(prefs_simple.get("baseDirectory",System.getProperty("user.home")));
        txtfldExtension.setText(prefs_simple.get("fileExtension","tif"));
        chkboxFlipHorizontal.setSelected(prefs_simple.getBoolean("flipHorizontal",true));
        chkboxTimeSeries.setSelected(prefs_simple.getBoolean("useSets",true));
        chkboxDayZero.setSelected(prefs_simple.getBoolean("haveDayZeroImage",true));
        sliderNumThreads.setValue(prefs_simple.getDouble("numThreads",1.0));

        txtfldExpIdentifier.setText(prefs_expert.get("experimentIdentifier","^(.*?_)"));
        txtfldSetIdentifier.setText(prefs_expert.get("setIdentifier","set\\d+"));
        txtfldDayIdentifier.setText(prefs_expert.get("dayIdentifier","day\\d+"));
        txtfldPlateIdentifier.setText(prefs_expert.get("plateIdentifier","\\d{3}$"));
        txtfldImageResolution.setText(prefs_expert.get("resolution","1200"));
        txtfldSeedMinSize.setText(prefs_expert.get("seedMinimumSize","0.10"));
        txtfldSeedMaxSize.setText(prefs_expert.get("seedMaximumSize","0.70"));
        txtfldShootMinThickness.setText(prefs_expert.get("shootMinThickness","0.1"));
        txtfldShootWidthStep.setText(prefs_expert.get("shootWidthStep","200"));
        txtfldShootHeightStep.setText(prefs_expert.get("shootHeightStep","200"));
        txtfldPlantMinThickness.setText(prefs_expert.get("plantMinThickness","0.1"));
        txtfldPlantMinHeight.setText(prefs_expert.get("plantMinHeight","0.1"));
        txtfldPlantWidthStep.setText(prefs_expert.get("plantWidthStep","200"));
        txtfldPlantHeightStep.setText(prefs_expert.get("plantHeightStep","200"));
        txtfldPlantTreeMaxLevel.setText(prefs_expert.get("PLANT_TREE_MAXLEVEL","10"));
        txtfldPlantTrackback.setText(prefs_expert.get("PLANT_TRACKBACK_SIZE","10"));
        txtfldCircleDiameter.setText(prefs_expert.get("CIRCLEDIAMETER","10"));
        txtfldLabelSize.setText(prefs_expert.get("LABEL_SIZE","30"));
        txtfldLabelFont.setText(prefs_expert.get("LABEL_FONT","sansserif-BOLD-30"));
        txtfldNumberSize.setText(prefs_expert.get("DEFAULTNUMBERSIZE","150"));
        txtfldNumberFont.setText(prefs_expert.get("DEFAULTNUMBERFONT","sansserif-BOLD-150"));
        clrpckShootRoi.setValue(Color.web(prefs_expert.get("SHOOTROI_COLOR","#00FF00")));
        clrpckRootRoi.setValue(Color.web(prefs_expert.get("ROOTROI_COLOR","#1E90FF")));
        clrpckSkeleton.setValue(Color.web(prefs_expert.get("SKELETON_COLOR","#808080")));
        clrpckMainRoot.setValue(Color.web(prefs_expert.get("MAINROOT_COLOR","#FF00FF")));
        clrpckStartPoint.setValue(Color.web(prefs_expert.get("STARTPT_COLOR","#FFB366")));
        clrpckEndPoint.setValue(Color.web(prefs_expert.get("ENDPT_COLOR","#1E90FF")));
        clrpckShootCM.setValue(Color.web(prefs_expert.get("SHOOTCM_COLOR","#FF0000")));
        clrpckGeneral.setValue(Color.web(prefs_expert.get("GENERAL_COLOR","#FF0000")));
        choiceboxLogLevel.setItems(FXCollections.observableArrayList("OFF","SEVERE","WARNING","INFO","CONFIG","FINE","FINER","FINEST","ALL"));
        choiceboxLogLevel.setValue(prefs_expert.get("logLevel","INFO"));

        lblNumThreads.textProperty().bind(Bindings.format("Threads: %2.0f", sliderNumThreads.valueProperty()));
        lblNumThreads.setStyle("-fx-font-family: monospace;");
        logBox.setLogQueue(logQueue);

        choiceboxLogLevel.getSelectionModel().selectedItemProperty().addListener((observableValue, o, t1) -> {
            Level newLogLevel = Level.parse(t1);
            Enumeration<String> loggerNames = LogManager.getLogManager().getLoggerNames();
            while(loggerNames.hasMoreElements()){
                String name = loggerNames.nextElement();
                if(name.contains("at.ac.oeaw.gmi.brat")){
                    Logger l = Logger.getLogger(name);
                    for(Handler h:l.getHandlers()){
                        if(h instanceof ConsoleHandler){
                            h.setLevel(newLogLevel);
                        }
                    }
                }
            }
        });

        accordionMain.expandedPaneProperty().addListener((observableValue, o, t1) -> {
            if(o != null){
                o.setCollapsible(true);
            }
            if(t1 != null){
                Platform.runLater(() -> t1.setCollapsible(false));
            }
        });
    }

    @FXML
    protected void directorySelect(ActionEvent e){
        String currentSelection = txtfldBaseDir.getText();
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Base Directory");
        if(new File(currentSelection).isDirectory()){
            chooser.setInitialDirectory(new File(currentSelection));
        }
        else{
            chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        }
        File dir = chooser.showDialog(txtfldBaseDir.getScene().getWindow());
        if(dir != null) {
            txtfldBaseDir.setText(dir.getAbsolutePath());
        }
    }

    @FXML
    protected void startRun(ActionEvent e){
        updatePreferences();
        accordionMain.setExpandedPane(paneLog);
        BratDispatcher dispatcher = new BratDispatcher();
        dispatcher.runFromGUI();
    }

    private void updatePreferences(){
                /*
         * simple
         */
        prefs_simple.put("baseDirectory",txtfldBaseDir.getText());
        prefs_simple.put("fileExtension",txtfldExtension.getText());
        prefs_simple.putBoolean("flipHorizontal",chkboxFlipHorizontal.isSelected());
        prefs_simple.putBoolean("useSets",chkboxTimeSeries.isSelected());
        prefs_simple.putBoolean("haveDayZeroImage",chkboxDayZero.isSelected());
        prefs_simple.putDouble("numThreads",sliderNumThreads.getValue());



        /*
	     * general
	     */
        prefs_expert.put("experimentIdentifier",txtfldExpIdentifier.getText());
        prefs_expert.put("setIdentifier",txtfldSetIdentifier.getText());
        prefs_expert.put("dayIdentifier",txtfldDayIdentifier.getText());
        prefs_expert.put("plateIdentifier",txtfldPlateIdentifier.getText());
        prefs_expert.put("logLevel",choiceboxLogLevel.getValue());
        prefs_expert.put("resolution",txtfldImageResolution.getText());
//        prefs_expert.putDouble("mmPerPixel",25.4/1200.0);
//
//        prefs_expert.put("outputDirectory",new File(System.getProperty("user.home"),"processed").getAbsolutePath());
//        prefs_expert.put("moveDirectory",new File(System.getProperty("user.home"),"processed-tif").getAbsolutePath());
//        prefs_expert.put("dayZeroIdentifier","day0");

        /*
         * seed detection
         */
        prefs_expert.put("seedMinimumSize",txtfldSeedMinSize.getText()); // minimum seed diameter [mm]
        prefs_expert.put("seedMaximumSize",txtfldSeedMaxSize.getText()); // maximum seed diameter [mm]

        /*
         * shoot detection
         */
        prefs_expert.put("shootMinThickness",txtfldShootMinThickness.getText()); // minimum thickness of shoot parts [mm]
        prefs_expert.put("shootWidthStep",txtfldShootWidthStep.getText()); // shoot area width increase
        prefs_expert.put("shootHeightStep",txtfldShootHeightStep.getText()); // shoot area height increase

        /*
         * root detection
         */
        prefs_expert.put("PLANT_TREE_MAXLEVEL",txtfldPlantTreeMaxLevel.getText());
        prefs_expert.put("PLANT_TRACKBACK_SIZE",txtfldPlantTrackback.getText()); // number of pixels included in calculation of tracking properties (e.g. tracking angle)
        prefs_expert.put("plantWidthStep",txtfldPlantWidthStep.getText());
        prefs_expert.put("plantHeightStep",txtfldPlantHeightStep.getText());
        prefs_expert.put("plantMinThickness",txtfldPlantMinThickness.getText());
        prefs_expert.put("plantMinHeight",txtfldPlantMinHeight.getText());

        /*
         * diagnostics
         */
        prefs_expert.put("SHOOTROI_COLOR",colorToHex(clrpckShootRoi.getValue())); //green
        prefs_expert.put("ROOTROI_COLOR",colorToHex(clrpckRootRoi.getValue())); //blue
        prefs_expert.put("SKELETON_COLOR",colorToHex(clrpckSkeleton.getValue())); //gray
        prefs_expert.put("MAINROOT_COLOR",colorToHex(clrpckMainRoot.getValue())); //magenta
        prefs_expert.put("STARTPT_COLOR",colorToHex(clrpckStartPoint.getValue())); //orange
        prefs_expert.put("ENDPT_COLOR",colorToHex(clrpckEndPoint.getValue())); //blue new Color(65280); //green Color.red;
        prefs_expert.put("SHOOTCM_COLOR",colorToHex(clrpckShootCM.getValue())); //red
        prefs_expert.put("GENERAL_COLOR",colorToHex(clrpckGeneral.getValue())); //red
        prefs_expert.put("LABEL_SIZE",txtfldLabelSize.getText());
        prefs_expert.put("LABEL_FONT",txtfldLabelFont.getText());
        prefs_expert.put("CIRCLEDIAMETER",txtfldCircleDiameter.getText());
        prefs_expert.put("DEFAULTNUMBERSIZE",txtfldNumberSize.getText());
        prefs_expert.put("DEFAULTNUMBERFONT",txtfldNumberFont.getText());

        try {
            prefs_simple.flush();
            prefs_expert.flush();
            log.fine("Updated Preferences.");
        } catch (BackingStoreException e) {
            log.severe(ExceptionLog.StackTraceToString(e));
            e.printStackTrace();
        }

    }

    private static String colorToHex(Color color) { return String.format("#%02X%02X%02X",
            (int)(color.getRed()*255),
            (int)(color.getGreen()*255),
            (int)(color.getBlue()*255));
    }
//    @FXML
//    protected void updateNumThreadsLabel(){
//        lblNumThreads.setText(String.format("Thread Number: %2d", (int)sliderNumThreads.getValue()));
//    }
}

//class LogLevelListener implements ChangeListener {
//    @Override
//    public void changed(ObservableValue observableValue, Object o, Object t1) {
//        Level newLogLevel = Level.parse((String)t1);
//        Enumeration<String> loggerNames = LogManager.getLogManager().getLoggerNames();
//        while(loggerNames.hasMoreElements()){
//            String name = loggerNames.nextElement();
//            if(name.contains("at.ac.oeaw.gmi.brat")){
//                Logger l = Logger.getLogger(name);
//                for(Handler h:l.getHandlers()){
//                    if(h instanceof ConsoleHandler){
//                        h.setLevel(newLogLevel);
//                    }
//                }
//            }
//        }
//    }
//}
