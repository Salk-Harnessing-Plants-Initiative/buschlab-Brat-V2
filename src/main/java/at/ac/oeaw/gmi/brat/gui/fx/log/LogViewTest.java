package at.ac.oeaw.gmi.brat.gui.fx.log;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by christian.goeschl on 10/17/16.
 */
public class LogViewTest extends Application{
	private final static Logger logger=Logger.getLogger(LogViewTest.class.getName());
    private final static LogQueue queue=new LogQueue(1_000_000);
    private final static BratFxLogHandler handler=new BratFxLogHandler(queue);

    public LogViewTest(){
        for(Handler h:logger.getHandlers()){
            if(h instanceof BratFxLogHandler){
                logger.removeHandler(h);
            }
        }
        logger.setLevel(Level.ALL);
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
    }


    @Override
    public void start(Stage stage) throws Exception {

        logger.info("Hello1");
        logger.warning("Don't pick up alien hitchhickers1");

         for (int x = 0; x < 2; x++) {
             Thread generatorThread = new Thread(
                     () -> {
                         for (; ; ) {
                             logger.fine("fine");
//                             logger.finer("finer");
//                             logger.finest("finest");
//                             logger.log(Level.INFO, "test");
//                             logger.severe("severe");
//                             logger.warning("warning");
//                             logger.info("info");
//                             logger.config("config");
                             try {
                                 Thread.sleep(500);
                             } catch (InterruptedException e) {
                                 e.printStackTrace();
                             }
                         }
                     }
             );
             generatorThread.setDaemon(true);
             generatorThread.start();
         }

//        LogView logView = new LogView(queue);
//        logView.setPrefWidth(400);
//
//        ChoiceBox<String> filterLevel = new ChoiceBox<>(
//                FXCollections.observableArrayList("OFF","SEVERE","WARNING","INFO","CONFIG","FINE","FINER","FINEST","ALL")
//        );
//
//        filterLevel.getSelectionModel().select("ALL");
//        logView.filterLevelProperty().bind(
//                filterLevel.getSelectionModel().selectedItemProperty()
//        );
//
//        ToggleButton showTimestamp = new ToggleButton("Show Timestamp");
//        logView.showTimeStampProperty().bind(showTimestamp.selectedProperty());
//
//        ToggleButton tail = new ToggleButton("Tail");
//        logView.tailProperty().bind(tail.selectedProperty());
//
//        ToggleButton pause = new ToggleButton("Pause");
//        logView.pausedProperty().bind(pause.selectedProperty());
//
//        Slider rate = new Slider(0.1, 60, 60);
//        logView.refreshRateProperty().bind(rate.valueProperty());
//        Label rateLabel = new Label();
//        rateLabel.textProperty().bind(Bindings.format("Update: %.2f fps", rate.valueProperty()));
//        rateLabel.setStyle("-fx-font-family: monospace;");
//        VBox rateLayout = new VBox(rate, rateLabel);
//        rateLayout.setAlignment(Pos.CENTER);
//
//        HBox controls = new HBox(
//                10,
//                filterLevel,
//                showTimestamp,
//                tail,
//                pause,
//                rateLayout
//        );
//        controls.setMinHeight(HBox.USE_PREF_SIZE);
//
//        VBox layout = new VBox(
//                10,
//                controls,
//                logView
//        );
//        VBox.setVgrow(logView, Priority.ALWAYS);

        LogBox logBox=new LogBox();
        logBox.setLogQueue(queue);
        Scene scene = new Scene(logBox);
        scene.getStylesheets().add(
                this.getClass().getClassLoader().getResource("log-view.css").toExternalForm()
        );
        stage.setScene(scene);
        stage.show();




        logger.info("********************Hello");
        logger.warning("*********************Don't pick up alien hitchhickers");
        logger.severe("***********************Severe");
   }

    public static void main(String[] args) {
        new LogViewTest();

        launch(args);
    }
}
