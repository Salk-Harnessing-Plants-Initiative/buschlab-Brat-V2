package at.ac.oeaw.gmi.brat.gui.fx.log;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.logging.LogRecord;


/**
 * Created by christian.goeschl on 10/17/16.
 *
 */
public class LogBox extends VBox {
    private LogView logView;
    private Button export;

    public LogBox(){
        super();
        logView = new LogView();
        ChoiceBox<String> filterLevel = new ChoiceBox<>(
                FXCollections.observableArrayList("OFF","SEVERE","WARNING","INFO","CONFIG","FINE","FINER","FINEST","ALL")
        );

        filterLevel.getSelectionModel().select("INFO");
        filterLevel.setPrefWidth(100);
        logView.filterLevelProperty().bind(
            filterLevel.getSelectionModel().selectedItemProperty()
        );

        ToggleButton showTimestamp = new ToggleButton("Extended Messages");
            logView.showTimeStampProperty().bind(showTimestamp.selectedProperty());

        ToggleButton tail = new ToggleButton("Tail");
            tail.setPrefWidth(100);
            logView.tailProperty().bind(tail.selectedProperty());
        tail.setSelected(true);

        ToggleButton pause = new ToggleButton("Pause");
        pause.setPrefWidth(100);
            logView.pausedProperty().bind(pause.selectedProperty());

        export = new Button("Export");
        export.setPrefWidth(100);

//        Slider rate = new Slider(0.1, 60, 60);
//            logView.refreshRateProperty().bind(rate.valueProperty());
//        Label rateLabel = new Label();
//            rateLabel.textProperty().bind(Bindings.format("Update: %.2f fps", rate.valueProperty()));
//            rateLabel.setStyle("-fx-font-family: monospace;");
//        VBox rateLayout = new VBox(rate, rateLabel);
//            rateLayout.setAlignment(Pos.CENTER);

        HBox controls = new HBox(
                10,
                filterLevel,
                showTimestamp,
                tail,
                pause,
                export
//                rateLayout
        );
        controls.setMinHeight(HBox.USE_PREF_SIZE);

//        VBox layout = new VBox(
//            10,
//            controls,
//            logView
//        );
//        VBox.setVgrow(logView, Priority.ALWAYS);

        getChildren().addAll(logView,controls);
        setVgrow(logView, Priority.ALWAYS);
    }

    public void setLogQueue(LogQueue logQueue){
        logView.setLogQueue(logQueue);
    }
    public ObservableList<LogRecord> getLogItems(){
        return logView.logItems;
    }
    public void setOnExportAction(EventHandler<ActionEvent> eventHandler){
        export.setOnAction(eventHandler);
    }
}
