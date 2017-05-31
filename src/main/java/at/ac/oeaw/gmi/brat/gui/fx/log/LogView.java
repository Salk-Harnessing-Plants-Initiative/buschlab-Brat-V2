package at.ac.oeaw.gmi.brat.gui.fx.log;

import com.sun.javafx.scene.control.skin.ListViewSkin;
import com.sun.javafx.scene.control.skin.VirtualFlow;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Duration;

import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Created by christian.goeschl on 10/17/16.
 *
 */


class LogView extends ListView<LogRecord> {
    private static final int MAX_ENTRIES = 10_000_000;

    private final static PseudoClass finest = PseudoClass.getPseudoClass("finest");
    private final static PseudoClass finer  = PseudoClass.getPseudoClass("finer");
    private final static PseudoClass fine = PseudoClass.getPseudoClass("fine");
    private final static PseudoClass config = PseudoClass.getPseudoClass("config");
    private final static PseudoClass info = PseudoClass.getPseudoClass("info");
    private final static PseudoClass warning  = PseudoClass.getPseudoClass("warning");
    private final static PseudoClass severe = PseudoClass.getPseudoClass("severe");

    private final static SimpleDateFormat timestampFormatter = new SimpleDateFormat("HH:mm:ss.SSS");

    private final BooleanProperty showTimestamp = new SimpleBooleanProperty(false);
    private final ObjectProperty<String> filterLevel   = new SimpleObjectProperty<>(null);
    private final BooleanProperty       tail          = new SimpleBooleanProperty(false);
    private final BooleanProperty       paused        = new SimpleBooleanProperty(false);
//    private final DoubleProperty refreshRate   = new SimpleDoubleProperty(60);

    protected final ObservableList<LogRecord> logItems = FXCollections.observableArrayList();
    private LogQueue logQueue;
    private Timeline logTransfer;

    BooleanProperty showTimeStampProperty() {
        return showTimestamp;
    }

    ObjectProperty<String> filterLevelProperty() {
        return filterLevel;
    }

    BooleanProperty tailProperty() {
        return tail;
    }

    BooleanProperty pausedProperty() {
        return paused;
    }

//    DoubleProperty refreshRateProperty() {
//        return refreshRate;
//    }

    LogView() {
        getStyleClass().add("log-view");

        logTransfer = new Timeline();
        logTransfer.setCycleCount(Timeline.INDEFINITE);
//        logTransfer.rateProperty().bind(refreshRateProperty());

        this.pausedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && logTransfer.getStatus() == Animation.Status.RUNNING) {
                logTransfer.pause();
            }

            if (!newValue && logTransfer.getStatus() == Animation.Status.PAUSED && getParent() != null) {
                logTransfer.play();
            }
        });

        this.parentProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                logTransfer.pause();
            } else {
                if (!paused.get()) {
                    logTransfer.play();
                }
            }
        });

        filterLevel.addListener((observable, oldValue, newValue) -> setItems(
                new FilteredList<>(
                        logItems,
                        logRecord ->
                                logRecord.getLevel().intValue() >=
                                        Level.parse(filterLevel.get()).intValue()
                )
        ));

        filterLevel.set("ALL");

        setCellFactory(param -> new ListCell<LogRecord>() {
            {
                showTimestamp.addListener(observable -> updateItem(this.getItem(), this.isEmpty()));
            }

            @Override
            protected void updateItem(LogRecord item, boolean empty) {
                super.updateItem(item, empty);

                pseudoClassStateChanged(finest, false);
                pseudoClassStateChanged(finer, false);
                pseudoClassStateChanged(fine, false);
                pseudoClassStateChanged(config, false);
                pseudoClassStateChanged(info, false);
                pseudoClassStateChanged(warning, false);
                pseudoClassStateChanged(severe, false);

                if (item == null || empty) {
                    setText(null);
                    return;
                }

                        //(item.getContext() == null) ? "" : item.getContext() + " ";

                if (showTimestamp.get()) {
                    String context = String.format("%s %s.%s@%d",item.getLevel(),item.getSourceClassName(),item.getSourceMethodName(),item.getThreadID());
                    String timestamp = timestampFormatter.format(item.getMillis());
                    setText(String.format("%s %s: %s",timestamp,context,item.getMessage()));
                } else {
                    setText(String.format("%s: %s",item.getLevel(),item.getMessage()));
                }

                switch (item.getLevel().getName()) {
                    case "FINEST":
                        pseudoClassStateChanged(finest, true);
                        break;
                    case "FINER":
                        pseudoClassStateChanged(finer, true);
                        break;
                    case "FINE":
                        pseudoClassStateChanged(fine, true);
                        break;
                    case "CONFIG":
                        pseudoClassStateChanged(config, true);
                        break;
                    case "INFO":
                        pseudoClassStateChanged(info, true);
                        break;
                    case "WARNING":
                        pseudoClassStateChanged(warning, true);
                        break;
                    case "SEVERE":
                        pseudoClassStateChanged(severe, true);
                        break;
                }
            }
        });
    }

//    public int getLast() {
//        try {
//            ListViewSkin<?> ts = (ListViewSkin<?>) getSkin();
//            VirtualFlow<?> vf = (VirtualFlow<?>) ts.getChildren().get(0);
//            int last = vf.getLastVisibleCell().getIndex();
//            System.out.println("last element = "+last);
//            return last;
////            logger.debug("##### Scrolling first {} last {}", first, last);
//        } catch (Exception ex) {
////            logger.debug("##### Scrolling: Exception " + ex);
//
//        }
//        return -1;
//    }


    public void setLogQueue(LogQueue logQueue){
        this.logQueue = logQueue;
        logTransfer.getKeyFrames().add(new KeyFrame(
                Duration.millis(100),
                event -> {
                    /*
                     * switch of automatic scrolling when scrollbar is moved from last position
                     * TODO: needs calculation of real shown elements if filter level is less than "ALL"
                     */
                    for(Node n : getChildrenUnmodifiable()){
                        if(n instanceof VirtualFlow){
                            VirtualFlow flow = (VirtualFlow)n;
//                            int first = 0;
                            int last = 0;
//                            IndexedCell cfirst = flow.getFirstVisibleCell();
                            IndexedCell clast = flow.getLastVisibleCell();

                            if(clast != null){
//                                first = cfirst.getIndex();
                                last = clast.getIndex();
                            }
//                            System.out.println(flow.getCellCount()+" - "+first+" - "+last+" - "+getItems().size());

                            int maxLen = getItems().size();
                            if(last == maxLen-1){
//                                System.out.println("tail on");
                                tail.setValue(true);
                            }
                            else{
//                                System.out.println("tail off");
                                tail.setValue(false);
                            }
                        }
                    }
                    logQueue.drainTo(logItems);

                    if (logItems.size() > MAX_ENTRIES) {
                        logItems.remove(0, logItems.size() - MAX_ENTRIES);
                    }

                    if (tail.get()) {
                        scrollTo(logItems.size());
                    }
                }
        )
        );
        logTransfer.play();
    }

    public void displayLog(){

    }
}
