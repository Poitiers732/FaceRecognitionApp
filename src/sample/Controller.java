package sample;

import java.awt.*;
import java.awt.TextArea;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.io.IOException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import org.opencv.core.*;
//import org.opencv.highgui.Highgui;
//import org.opencv.highgui.VideoCapture;
import org.opencv.core.Point;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.imageio.ImageIO;

import static com.googlecode.javacv.cpp.opencv_core.CV_8UC;
import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgproc.Imgproc.*;



/**
 * W klasie Controller znajduje sie logika aplikacji, obsługa streamu z kamery, buttonów itd.
 *
 * @author <a href="mailto:wojciech.zdro@gmail.com">Wojciech Zdrodowski</a>
 * @date 17.08.2017
 */

public class Controller implements Initializable{

    // FXML buttons
    @FXML
    private Button cameraButton;
    @FXML
    private Button recognizeButton;
    @FXML
    private Button snapshotButton;
    // the FXML area for showing the current frame (before calibration)
    @FXML
    private Button findInDatabase;
    @FXML
    private ImageView originalFrame;
    // info related to the calibration process
    @FXML
    private CheckBox threshCheck;
    @FXML
    private CheckBox grayCheck;
    @FXML
    private CheckBox camCheck;
    @FXML
    private Slider threshSlider;
    @FXML
    private TextField textField;
    @FXML
    private TextField textFieldDoKogo;
    @FXML
    private ImageView lastFrame;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label label;
    @FXML
    public ListView<String> listView;
    @FXML
    private TableView<Person> tableView;
    @FXML
    private TableColumn<Person, String> personName;
    @FXML
    private TableColumn<Person, Integer> personPercent;


    //inicjalizacja tableView

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        try {
            Scanner in = new Scanner(new FileReader("src/sample/snapshots/DB.txt"));

            while(in.hasNext()) {
                tableView.getItems().add(new Person(in.next(), 0));
            }
        }catch (Exception e){}

        personName.setCellValueFactory(new PropertyValueFactory<Person, String>("personName"));
        personPercent.setCellValueFactory(new PropertyValueFactory<Person, Integer>("personPercent"));
    }

    //usuwa wszystkie @Person z tableView i dodaje nowe, z uzupełnionymi wartościami procent pdobieństwa

    @FXML
    public void organizeTableViewPercent(){

        tableView.getItems().clear();

        String next = new String();

        try {
            Scanner inDB = new Scanner(new FileReader("src/sample/snapshots/DB.txt"));

            while(inDB.hasNext()) {

                next = inDB.next();
                System.out.println("next "+next);

                if(!next.equals(textField.getText())) {
                    tableView.getItems().add(new Person(next, comparePerson(next)));
                }

            }

        }catch (Exception e){
            //e.printStackTrace();
        }

        personName.setCellValueFactory(new PropertyValueFactory<Person, String>("personName"));
        personPercent.setCellValueFactory(new PropertyValueFactory<Person, Integer>("personPercent"));
    }

    /**
     * Zwraca wartość podobieństwa między osobą wpisaną do textField i osoby wpisanej do pliku tekstowego (np. personAngelina.txt)
     * @param personName jest pobierany z DB.txt jako string. Każdy plik txt jest nazwany np. personNazwaOsoby.txt. Są tam zapisane dane
     * o proporcjach punktów charakterystyczncyh twarzy.
     * @return
     */

    public int comparePerson(String personName){

        double recognizeResult = 0;
        double tempDouble1 = 0;
        double tempDouble2 = 0;

        String tempStr1 = new String();
        String tempStr2 = new String();

        try {
            Scanner in = new Scanner(new FileReader("src/sample/snapshots/" + "person" + textField.getText() + ".txt"));
            Scanner in2 = new Scanner(new FileReader("src/sample/snapshots/" + "person" + personName + ".txt"));

            //while (in.hasNext() && !in.next().equals("===") && !in2.next().equals("===")) {
            while(in.hasNext() && !in.equals("===")) {

                tempStr1 = in.next();
                tempStr2 = in2.next();

                tempDouble1 = Double.parseDouble(tempStr1);
                tempDouble2 = Double.parseDouble(tempStr2);

                if (tempDouble1 > tempDouble2) {
                    recognizeResult += (tempDouble1 - tempDouble2);
                    System.out.println("recognizeResult from fx: " + recognizeResult);

                } else {
                        recognizeResult += (tempDouble2 - tempDouble1);
                        System.out.println("recognizeResult from fx: " + recognizeResult);
                }
            }
            in.close();
            in2.close();
        } catch (Exception e) {
        }

        return (100-(int) recognizeResult);
    }

    // timer do video streamu
    private Timer timer;
    // obiekt OpenCV, który wykonuje video capture
    private VideoCapture capture;
    // flaga do buttona
    private boolean cameraActive;
    private Image CamStream;
    // various variables needed for the calibration

    private boolean oneFaceDetected = false;
    private boolean twoEyesDetected = false;
    private boolean rightEyeDetected = false;
    private boolean leftEyeDetected = false;
    private boolean smileDetected = false;
    private boolean leftEye2splitsDetected = false;
    private boolean everythingDetected = false;
    private boolean frontalFaceDetected = false;

    // punkty do wycinania snapshota twarzy
    private int rectx, recty, rect_width, rect_height = 0;

    // point for recognition
    double smilePointLeft = 0;
    double smilePointRight = 0;
    double smilePointLevel = 0;

    double noseX = 0;
    double noseY = 0;

    double leftEyePointLeft = 0;
    double leftEyePointRight = 0;
    double leftEyePointLevel = 0;

    double rightEyePointLeft = 0;
    double rightEyePointRight = 0;
    double rightEyePointLevel = 0;

    double pointDistances[] = new double[22];

/**
 * klasyfikatory
 */

    CascadeClassifier faceDetector = new CascadeClassifier("src/haars/lbpcascade_frontalface.xml");
    CascadeClassifier smileDetector = new CascadeClassifier("src/haars/Mouth2.xml");
    CascadeClassifier noseDetector = new CascadeClassifier("src/haars/Nariz.xml");
    CascadeClassifier rightEyeDetector = new CascadeClassifier("src/haars/haarcascade_lefteye_2splits.xml");
    CascadeClassifier leftEyeDetector = new CascadeClassifier("src/haars/haarcascade_righteye_2splits.xml");

    /**
     * Inicjowanie zmiennych globalnych
     */
    protected void init() {
        this.capture = new VideoCapture();
        this.cameraActive = false;
        this.cameraButton.setDisable(false);
    }



    /**
     * Start streamu z kamery
     */
    @FXML
    protected void startCamera() {


        if (!this.cameraActive) {
            // wlacza snapshot button
            this.snapshotButton.setDisable(false);

            // zaczyna przechwytywanie obrazów
            this.capture.open(0);

            // jeśli stream jest możliwy
            if (this.capture.isOpened()) {
                this.cameraActive = true;

                // klatki (30 frames/sec)
                TimerTask frameGrabber = new TimerTask() {
                    @Override
                    public void run() {
                        CamStream = grabFrame();
                        // pokaż klatki
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {

                                originalFrame.setImage(CamStream);
                                originalFrame.setFitWidth(900);
                                originalFrame.setPreserveRatio(true);
                            }
                        });

                    }
                };
                this.timer = new Timer();
                this.timer.schedule(frameGrabber, 0, 33);

                // update the button content
                this.cameraButton.setText("Stop Camera");
            } else {
                // log the error
                System.err.println("Camera connection f-uped");
            }
        } else {
            // the camera is not active at this point
            this.cameraActive = false;
            // update again the button content
            this.cameraButton.setText("Start Camera");
            // disable snapshot button
            //this.snapshotButton.setDisable(true);
            // wyłącz timer
            if (this.timer != null) {
                this.timer.cancel();
                this.timer = null;
            }
            // release cameraa
            this.capture.release();
        }
    }

    /**
     * Jeśli jest video stream, to pobierz kaltki (frame)
     *
     * @return  {@link Image}
     */
    private Image grabFrame() {
        // init everything
        Image imageToShow = null;
        Mat frame = new Mat();

        // check if the capture is open
        if (this.capture.isOpened()) {
            try {
                // read the current frame
                this.capture.read(frame);

                // if the frame is not empty, process it
                if (!frame.empty()) {


                    // convert the Mat object (OpenCV) to Image (JavaFX)

                    if (camCheck.isSelected()) {
                        frame = imread("src/sample/resource/4.jpg");

                    }

                    try {
                        faceDetect(frame);
                        eyeDetect(frame.submat(recty, recty + rect_height, rectx + rect_width / 2, rectx + rect_width), frame, new String("right"));
                        eyeDetect(frame.submat(recty, recty + rect_height, rectx, rectx + rect_width / 2), frame, new String("left"));
                        smileDetect(frame.submat(recty + rect_height * 2 / 3, recty + rect_height, rectx, rectx + rect_width), frame);
                        noseDetect(frame.submat(recty + rect_height * 1 / 2, recty + rect_height * 4 / 5,
                                rectx + rect_width * 1 / 5, rectx + rect_width * 4 / 5), frame);


                        /*
                        if (oneFaceDetected && rect_width > 5 && rect_height > 5 && rect_height * rect_width > 7) {
                            //eyeDetect(frame.submat(rectx, rectx + rect_width, recty, recty + rect_height));
                            //smileDetect(frame.submat( rectx, rectx + rect_width, recty , recty + rect_height));
                            //noseDetect(frame.submat( rectx, rectx + rect_width, recty  , recty + rect_height));
                            //Imgproc.circle(frame,new Point(rectx, recty+150), 25, new Scalar(255,255,255));
                        }
                        */


                    } catch (Exception e) {
                        System.err.print("ERROR");
                        e.printStackTrace();
                    }
                    imageToShow = mat2Image(frame);
                }

            } catch (Exception e) {
                // log the (full) error
                System.err.print("ERROR");
                e.printStackTrace();
            }
        }

        return imageToShow;
    }

    /**
     * robi Snapshota - plik .png wykrytej twarzy oraz pobiera współrzędne punktów charakterystycznych
     */
    @FXML
    protected void takeSnapshot() {
        setLandmarks();
        faceRecognition();

        // imie z textField
        String textFieldStr = textField.getText();

        //organizowanie danych o punktach charakterystycznych
        getLandmarksProportions(textFieldStr);

        calculateDistanceProportions();

        addPersonToDB();

        if (oneFaceDetected) {
            saveToFile(grabFrame(), rectx, recty, rect_width, rect_height, textFieldStr);
            snapshotButton.setStyle("-fx-base: #b6e7c9;");
            snapshotButton.setText("Snapshot succesful");
        } else {
            snapshotButton.setStyle("-fx-base: #FF0500;");
            snapshotButton.setText("Snapshot failed");
        }

        try {
            lastFrame.setImage(   saveToFile(grabFrame(), rectx, recty, rect_width, rect_height, textFieldStr ));
        }
        catch (Exception e){}


        //snapshotButton.setText("Take snapshot");
    }


    /**
     * Zapisuje osobę z textField w DB.txt (jeśli jeszcze w niej nie występuje
     */

    public void addPersonToDB(){

        String tempStr;
        Boolean alreadyInDB=false;
        try {
            Scanner in = new Scanner(new FileReader("src/sample/snapshots/" + "DB"  + ".txt"));

            while(in.hasNext()) {
                tempStr = (in.next());

                if(textField.getText().equals(tempStr)){
                    alreadyInDB = true;
                }
            }
        }
        catch (Exception e){
        }

        if(alreadyInDB==false){
            WriteFile(textField.getText(), "src/sample/snapshots/" + "DB"  + ".txt");
        }
    }


    /**
     * Konwertuje obiekt Mat (OpenCV) do Image (JavaFX)
     *
     * @param frame {@link Mat} klatka z streama
     * @return {@link Image} obraz do pokazania
     */
    private Image mat2Image(Mat frame) {
        // create a temporary buffer
        MatOfByte buffer = new MatOfByte();

        // for grayframe
       // Mat grayFrame = new Mat();
        //Mat thr = new Mat();


        //Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
        // Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2HSV);
        //Imgproc.equalizeHist(frame, frame);


        if (this.grayCheck.isSelected()) {
             Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
            // Imgproc.equalizeHist(frame, frame);

            //Imgproc.GaussianBlur(frame, frame, new Size(9, 9), 2, 2);
        }

        if (this.threshCheck.isSelected()) {
            Imgproc.threshold(frame, frame, threshSlider.getValue(), 255, THRESH_BINARY);
        }

        // encode the frame in the buffer, according to the PNG format
        Imgcodecs.imencode(".png", frame, buffer);

        // build and return an Image created from the image encoded in the
        // buffer

        return new Image(new ByteArrayInputStream(buffer.toArray()));

    }

    //Save an image
    public static Image saveToFile(Image image, int rectx, int recty, int rect_width, int rect_height, String textFieldStr) {
        File outputFile = new File("src/sample/snapshots/" + textFieldStr + " " + getDate() + ".png");
        BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
        //cropp image
        bImage = bImage.getSubimage(rectx, recty, rect_width, rect_height);

        try {
            ImageIO.write(bImage, "png", outputFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Image tempImage = SwingFXUtils.toFXImage(bImage, null);
        return tempImage;

         //pokazanie ostatniego Snapshota w ImageView lastFrame
        //Image imageLast = new Image(outputFile.toURI().toString());

        /*
        Image tempImage = SwingFXUtils.toFXImage(bImage, null);
        lastFrame = new ImageView();
        lastFrame.setImage(tempImage);
        lastFrame.setFitWidth(200);
        lastFrame.setPreserveRatio(true);
        */
    }



    //get date for the saved picture name
    public static String getDate() {
        //getting current date and time using Date class
        DateFormat df = new SimpleDateFormat("dd MM yy HH_mm_ss");
        Date dateobj = new Date();
       // System.out.println(df.format(dateobj));

       /*getting current date time using calendar class
        * An Alternative of above*/
        Calendar calobj = Calendar.getInstance();
        return (df.format(calobj.getTime()));
    }

    public boolean faceDetect(Mat image) {
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(image, faceDetections);


        if (faceDetections.toArray().length == 1) {
            oneFaceDetected = true;
        } else {
            oneFaceDetected = false;
        }

        // Draw a bounding box around each face.
        for (Rect rect : faceDetections.toArray()) {
            Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0));
            rectx = rect.x;
            recty = rect.y;
            rect_width = rect.width;
            rect_height = rect.height;

            // Imgproc.rectangle(image, new Point(rectx, recty), new Point(rectx+rect_width, recty + rect_height/2), new Scalar(255, 255, 255));

        }

        return oneFaceDetected;
    }

    public boolean eyeDetect(Mat image, Mat original, String eyeSide) {
        MatOfRect eyeDetections = new MatOfRect();

        if(eyeSide.equals("right")) {
            rightEyeDetector.detectMultiScale(image, eyeDetections);
        }

        if(eyeSide.equals("left")) {
            leftEyeDetector.detectMultiScale(image, eyeDetections);
        }


        if (eyeDetections.toArray().length == 1) {


            // rysowanie prostokatow wokół haarów.
            for (Rect rect : eyeDetections.toArray()) {
                // Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0));
                //Imgproc.circle(image, new Point(rect.x+rect.width/2, rect.y+rect.height/2), rect.height/3 , new Scalar(0, 0, 255) );

                Imgproc.line(image, new Point(rect.x, rect.y + rect.height / 2),
                        new Point(rect.x + rect.width, rect.y + rect.height / 2), new Scalar(255, 255, 0));

                Imgproc.circle(image, new Point(rect.x, rect.y + rect.height / 2), 2, new Scalar(255, 255, 0), 5);
                Imgproc.circle(image, new Point(rect.x + rect.width, rect.y + rect.height / 2), 2, new Scalar(255, 255, 0), 5);

                if (eyeSide.equals("right")) {

                    leftEyePointLeft = rect.x;
                    leftEyePointRight = rect.x + rect_width;
                    leftEyePointLevel = rect.y + rect.height / 2;

                    Imgproc.putText(original, eyeSide + "EYE: " + " " + leftEyePointLeft + " " + leftEyePointRight + " " + leftEyePointLevel, new Point(10, 30), Core.FONT_HERSHEY_PLAIN, 0.5, new Scalar(255, 255, 255));
                }

                if (eyeSide.equals("left")) {
                    Imgproc.putText(original, eyeSide + "EYE: " + " " + rightEyePointLeft + " " + rightEyePointRight + " " + rightEyePointLevel, new Point(10, 40), Core.FONT_HERSHEY_PLAIN, 0.5, new Scalar(255, 255, 255));

                    rightEyePointLeft = rect.x;
                    rightEyePointRight = rect.x + rect_width;
                    rightEyePointLevel = rect.y + rect.height / 2;
                }
            }

            if(eyeSide.equals("right")) {
                rightEyeDetected = true;
            }

            if(eyeSide.equals("left")) {
                leftEyeDetected = true;
            }

        } else {
            if(eyeSide.equals("right")) {
                rightEyeDetected = false;
            }

            if(eyeSide.equals("left")) {
                leftEyeDetected = false;
            }
        }

        if(rightEyeDetected == false || leftEyeDetected == false){
            twoEyesDetected = false;
        }
        else{
            twoEyesDetected = true;
        }
            return twoEyesDetected;
    }


    public boolean noseDetect(Mat image, Mat original) {
        MatOfRect noseDetections = new MatOfRect();
        noseDetector.detectMultiScale(image, noseDetections);
        boolean noseDetected;

        if (noseDetections.toArray().length == 1) {
            noseDetected = true;

            // Draw a bounding box around each eye.
            for (Rect rect : noseDetections.toArray()) {
                //Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 0));
                //Imgproc.circle(image, new Point(rect.x+rect.width/2, rect.y+rect.height/2), rect.height/3 , new Scalar(0, 0, 255) );

            /*
            Imgproc.line(image,  new Point(rect.x, rect.y+rect.height/2),
                    new Point(rect.x+rect.width, rect.y+rect.height/2), new Scalar(0,0,0));


            Imgproc.circle(image,  new Point(rect.x, rect.y+rect.height/2), 2, new Scalar(0,0,0), 5);
            Imgproc.circle(image,  new Point(rect.x+rect.width, rect.y+rect.height/2), 2, new Scalar(0,0,0), 5);
            */

                Imgproc.circle(image, new Point(rect.x + rect.width / 2, rect.y + rect.height / 2), 2, new Scalar(0, 0, 0), 5);
                noseX = rect.x + rect_width / 2;
                noseY = rect.y + rect.height / 2;

                Imgproc.putText(original, "NOSE: " + noseX + " " + noseY, new Point(10, 20), Core.FONT_HERSHEY_PLAIN, 0.5, new Scalar(255, 255, 255));

            }
        } else {
            noseDetected = false;
        }

        return noseDetected;
    }

    public boolean leftEye2splitsDetect(Mat image) {
        MatOfRect leftEye2splitsDetections = new MatOfRect();
        leftEyeDetector.detectMultiScale(image, leftEye2splitsDetections);

        if (leftEye2splitsDetections.toArray().length == 2) {
            twoEyesDetected = true;
        } else {
            twoEyesDetected = false;
            // Draw a bounding box around each eye.
            for (Rect rect : leftEye2splitsDetections.toArray()) {
                Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 255));
                //Imgproc.circle(image, new Point(rect.x+rect.width/2, rect.y+rect.height/2), rect.height/3 , new Scalar(0, 0, 255) );
            }
        }

        if (leftEye2splitsDetections.toArray().length == 2) {
            leftEye2splitsDetected = true;
          //  System.out.println("leftEye2splitsDetected=true");
        } else {
            leftEye2splitsDetected = false;
        }

        return leftEye2splitsDetected;
    }

    public boolean smileDetect(Mat image, Mat original) {
        MatOfRect smileDetections = new MatOfRect();
        smileDetector.detectMultiScale(image, smileDetections);

        if (smileDetections.toArray().length == 1) {

            for (Rect rect : smileDetections.toArray()) {

                //Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 255));
                Imgproc.line(image, new Point(rect.x, rect.y + rect.height / 2),
                        new Point(rect.x + rect.width, rect.y + rect.height / 2), new Scalar(0, 255, 255));

                Imgproc.circle(image, new Point(rect.x, rect.y + rect.height / 2), 2, new Scalar(0, 255, 255), 5);
                Imgproc.circle(image, new Point(rect.x + rect.width, rect.y + rect.height / 2), 2, new Scalar(0, 255, 255), 5);

                smilePointLeft = rect.x;
                smilePointRight = rect.x + rect_width;
                smilePointLevel = rect.y + rect.height / 2;
            }

            smileDetected = true;
            Imgproc.putText(original, "MOUTH: " + smilePointLeft + " " + smilePointRight + " " + smilePointLevel, new Point(10, 10), Core.FONT_HERSHEY_PLAIN, 0.5, new Scalar(255, 255, 255));


        } else {
            smileDetected = false;
        }

        return smileDetected;
    }

    public void faceRecognition() {
        setPoints();
        for (int i = 0; i < 7; i++) {
            for (int u = 0; u < 7; u++) {

                if(i>=u) {
                 //   System.out.println(((setLandmarks().get("m"+i)) + "\t ") );
                }
            }
            System.out.println();
        }
    }

    public double distance(Point punkt1, Point punkt2) {
        double dx = punkt1.x - punkt2.x;
        double dy = punkt1.y - punkt2.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance;
    }

    public void setPoints(){
        /*
        p0 = new Point(smilePointLeft, smilePointLevel);
        p1 = new Point(smilePointRight, smilePointLevel);
        p2 = new Point(noseX, noseY);
        p3 = new Point(LeftEyePointLeft, LeftEyePointLevel);
        p4 = new Point(LeftEyePointRight, LeftEyePointLevel);
        p5 = new Point(RightEyePointLeft, RightEyePointLevel);
        p6 = new Point(RightEyePointRight, RightEyePointLevel);
        */
    }

    public boolean everythingIsDetected(){

        if(
        oneFaceDetected == false &&
        twoEyesDetected == false &&
        rightEyeDetected == false &&
        leftEyeDetected == false &&
        smileDetected == false
        ){
            return true;
        }
        else{
            return false;
        }
    }

    public void getLandmarksProportions(String textFieldStr){
        Point tmpPoint1 = new Point(0,0);
        Point tmpPoint2 = new Point(0,0);

        Point tmpPoint3 = new Point(0,0);
        Point tmpPoint4 = new Point(0,0);

        Object tmpObject1;
        Object tmpObject2;

        Object tmpObject3;
        Object tmpObject4;

        File newTextFile = new File("src/sample/snapshots/" + "person_" + textFieldStr + ".txt");

        int x=0;

        for(int i=0;i<7;i++){
            for(int u=0;u<7;u++){
                if(i>u) {

                    tmpObject1 = setLandmarks().get("m" + u);
                    tmpObject2 = setLandmarks().get("m" + i);

                    tmpPoint1 = (Point) tmpObject1;
                    tmpPoint2 = (Point) tmpObject2;

                    tmpObject3 = setLandmarks().get("m" + u);
                    tmpObject4 = setLandmarks().get("m" + i);

                    tmpPoint3 = (Point) tmpObject3;
                    tmpPoint4 = (Point) tmpObject4;

                    //System.out.println(String.valueOf(distance(tmpPoint1, tmpPoint2)));

                    /*
                    double tempDouble1 = (distance(tmpPoint1, tmpPoint2));
                    double tempDouble2 = (distance(tmpPoint3, tmpPoint4));
                    double divide;

                    DecimalFormat twoDForm = new DecimalFormat("#.0");
                    tempDouble1 = Double.valueOf(twoDForm.format(tempDouble1));
                    tempDouble2 = Double.valueOf(twoDForm.format(tempDouble2));
*/

                    String tempStr;
                    tempStr = String.valueOf(distance(tmpPoint1, tmpPoint2));
                    tempStr = tempStr.substring(0, tempStr.indexOf("."));

                    double tempDistance = distance(tmpPoint1, tmpPoint2);

                    DecimalFormat df = new DecimalFormat("#");
                    //tempDistance = Double.parseDouble(df.format(tempDistance));

                    //WriteFile("odleglosc miedzy punktami: "+ u+""+i+"|"+ tempStr );
                   // pointDistances[x] = Double.parseDouble( tempStr );
                    pointDistances[x] = Double.parseDouble( tempStr );
                    System.out.println("tempStr "+tempStr);
                    System.out.println("point distances "+x+" "+pointDistances[x]);

                    x++;

                    /*
                    int p=0;
                    while(p<21) {
                        pointDistances[p] = tempDistance;
                        p++;
                    }

                    for(int a=0;a<27;a++){
                        System.out.println("point distances "+pointDistances[a]);
                    }
                    */
                }
            }
        }
        //WriteFile("---");
    }


    /**
     *  dzieli wszystkie elementy @pointDistances[] przez siebie bez powtorzen w celu otrzymania proporcji
     *  pomiędzy wszystkimi punktami charakterystycznymi, a później zapisuje do do pliku txt
     */

    public void calculateDistanceProportions() {

        double proportions;

        for(int i=0;i<21;i++) {
            for (int u = 0; u < 21; u++) {
                if (i > u) {
                    proportions = pointDistances[i] / pointDistances [u];
                    System.out.println("proportions"+" "+i+" "+u+proportions);
                    WriteFile(String.valueOf(proportions),"src/sample/snapshots/" +"person" + textField.getText() + ".txt");
                }
            }
        }
        WriteFile("---","src/sample/snapshots/" +"person" + textField.getText() + ".txt");
    }


    public void WriteFile(String string, String filename) {

       // String filename = "src/sample/snapshots/" +"person" + textField.getText() + ".txt";

        BufferedWriter bw = null;
        FileWriter fw = null;

        try {

            String content = string + "\n";

            fw = new FileWriter(filename, true);
            bw = new BufferedWriter(fw);
            bw.write(content);

          //  System.out.println("Done");

        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            try {

                if (bw != null)
                    bw.close();

                if (fw != null)
                    fw.close();

            } catch (IOException ex) {

                ex.printStackTrace();
            }
        }
    }

    public static void savePersonLandmarks(String string, String textFieldStr, File newTextFile){
      //  "src/sample/snapshots/" + "person" + textFieldStr + ".txt")



        try {


            FileWriter fw = new FileWriter(newTextFile);
            fw.write(string);
            fw.close();

        } catch (IOException iox) {
            //do stuff with exception
            iox.printStackTrace();
        }
    }


    /**
     *  zapis punktów charakterystycznych do macierzy p[] oraz później do mapy landamrkPointMap
     */

    public Map setLandmarks()
     {
            /* landmarkPoints:
    1 - smile left
    2 - smile right
    3 - nose
    4 - left eye left corner
    5 - left eye right corner
    6 - right eye left corner
    7 - right eye right corner
    */

        // macierz punktow 1-7

        Map<String, Point> landmarkPointMap = new HashMap<String, Point>();

        Point p[] = new Point[7];
        p[0] = new Point(smilePointLeft, smilePointLevel);
        p[1] = new Point(smilePointRight, smilePointLevel);
        p[2] = new Point(noseX, noseY);
        p[3] = new Point(leftEyePointLeft, leftEyePointLevel);
        p[4] = new Point(leftEyePointRight, leftEyePointLevel);
        p[5] = new Point(rightEyePointLeft, rightEyePointLevel);
        p[6] = new Point(rightEyePointRight, rightEyePointLevel);


        for(int i=0;i<7;i++) {
            landmarkPointMap.put("m" + i, p[i]);
           // System.out.println("point p"+i+" "+p[i]+" ");
        }
        return landmarkPointMap;
    }

    @FXML
    public String getNameToTable(){

        String name = new String("");

        try {
            Scanner in = new Scanner(new FileReader("src/sample/snapshots/DB.txt"));

            while (in.hasNext()) {
                name = in.next();
            }
        } catch (Exception e) {
        }
        return name;
    }

    /**
     * Funkcja działa po wciśnięciu buttona Recognize. Pobiera stringa z pola tekstowego textFieldDoKogo i porównuje to z textField
     */

    @FXML
    public void recognizeFromTxt(){

        double recognizeResult = 0;
        double tempDouble1 = 0;
        double tempDouble2 = 0;

        String tempStr1;
        String tempStr2;

        try {
            Scanner in = new Scanner(new FileReader("src/sample/snapshots/" + "person" + textField.getText() + ".txt"));
            Scanner in2 = new Scanner(new FileReader("src/sample/snapshots/" + "person" + textFieldDoKogo.getText() + ".txt"));

       // StringBuilder sb = new StringBuilder();
       // StringBuilder sb2 = new StringBuilder();

         //   tempStr1 = sb.toString();
          //  tempStr2 = sb2.toString();

            tempStr1 = new String("");
            tempStr2 = new String("");

        while(in.hasNext() && !in.equals("===")) {
          tempStr1 = (in.next());
          tempStr2 = (in2.next());

            tempDouble1 =  Double.parseDouble(tempStr1);
            tempDouble2 =  Double.parseDouble(tempStr2);

            if(tempDouble1>tempDouble2) {
                recognizeResult += (tempDouble1 - tempDouble2);
                System.out.println("result "+recognizeResult);
            }
            else {
                recognizeResult += (tempDouble2 - tempDouble1);
                System.out.println("result "+recognizeResult);
            }
        }
            in.close();
            in2.close();
        }
        catch(Exception e){
            System.out.println("Error recognizeFromTxt");
        }

        progressBar.setProgress((100-recognizeResult)/100);
        progressBar.setAccessibleText(Double.toString(100-recognizeResult));

        String result = String.format("%.2f", (100-recognizeResult));

        String jakiePodobienstwo = new String("(NISKIE)");

        progressBar.setStyle("-fx-accent: red;");
        if((100-recognizeResult)>50) {
            progressBar.setStyle("-fx-accent: yellow;");
            jakiePodobienstwo = "ŚREDNIE";
        }
        if((100-recognizeResult)>75) {
            progressBar.setStyle("-fx-accent: green;");
            jakiePodobienstwo = "WYSOKIE";
        }
        if(recognizeResult==0){
            jakiePodobienstwo = "IDENTYCZNE";
            result = "100";
            progressBar.setStyle("-fx-accent: green;");
            progressBar.setProgress(100.0);
        }


        label.setText("Podobienstwo pomiedzy " + textField.getText() + " oraz " + textFieldDoKogo.getText() + " to "+result + " " +  jakiePodobienstwo);

        if(recognizeResult!=0) {
            System.out.println("Podobienstwo pomiedzy " + textField.getText() + " oraz " + textFieldDoKogo.getText() + " to " + result + "% |" + recognizeResult);
        }


        else if(recognizeResult>100){
            System.out.println("Podobienstwo pomiedzy " + textField.getText() + " oraz " + textFieldDoKogo.getText() + " to " + result + "% |" + recognizeResult);
            progressBar.setStyle("-fx-accent: red;");
            progressBar.setProgress(0.03);
        }
    }
}
