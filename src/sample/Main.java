package sample;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.opencv.core.Core;

//import org.opencv.highgui.Highgui;
//import org.opencv.highgui.VideoCapture;

//imports

import javafx.scene.layout.BorderPane;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {

        try {
            // load the FXML resource
            FXMLLoader loader = new FXMLLoader(getClass().getResource("sample.fxml"));
            // store the root element so that the controllers can use it
            BorderPane rootElement = (BorderPane) loader.load();
            // set a whitesmoke background
            rootElement.setStyle("-fx-background-color: whitesmoke;");
            // create and style a scene
            Scene scene = new Scene(rootElement, 1600, 1000);
            scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
            // create the stage with the given title and the previously created
            // scene
            primaryStage.setTitle("Face Recognition");
            primaryStage.setScene(scene);
            // init the controller variables
            Controller controller = loader.getController();
            controller.init();
            // show the GUI
            primaryStage.show();
        } catch(Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        launch(args);
    }
}
