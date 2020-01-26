package com.justfors;

import com.justfors.client.Client;
import com.justfors.client.NetConnectionClient;
import com.justfors.protocol.TransferData;
import com.justfors.stream.InputStream;
import com.justfors.stream.OutputStream;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Main extends Application implements NetConnectionClient {

    private static final Double WIDTH = 300.0;
    private static final Double HEIGHT = 400.0;
    private static final Double PLAYER_BOX_WIDTH = 10.0;
    private static final Double PLAYER_BOX_HEIGHT = 10.0;
    private static final Double BULLET_BOX_WIDTH = 4.0;
    private static final Double BULLET_BOX_HEIGHT = 4.0;


    private static final String NICKNAME = "PLAYER" + Math.random()*10000;

    private static volatile Pane root;
    private static volatile Stage stage;
    private static volatile Rectangle rect;
    private static volatile Scene scene;

    private static final Map<String, Rectangle> playerBoxesMap = new ConcurrentHashMap<>();
    private static final Map<String, Rectangle> bulletMap = new ConcurrentHashMap<>();

    {
        root = new Pane();
        rect = initNewRectangle();
        playerBoxesMap.put(NICKNAME, rect);
        scene = new Scene(root, WIDTH, HEIGHT);
    }

    public static void main(String[] args) {
        Main main = new Main();
        new Client("93.125.42.194", 7777,main).start();
        main.login();
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setMinWidth(WIDTH);
        stage.setMinHeight(HEIGHT);
        stage.setTitle("JFDC");

        root.getChildren().add(playerBoxesMap.get(NICKNAME));

        scene.setOnKeyPressed((e) -> {
            switch (e.getCode()) {
                case UP:    sendMessage(playerBoxesMap.get(NICKNAME), "UP"); break;
                case DOWN:  sendMessage(playerBoxesMap.get(NICKNAME), "DOWN"); break;
                case LEFT:  sendMessage(playerBoxesMap.get(NICKNAME), "LEFT"); break;
                case RIGHT: sendMessage(playerBoxesMap.get(NICKNAME), "RIGHT"); break;
                case SHIFT: sendMessage(playerBoxesMap.get(NICKNAME), "SHOT"); break;
            }
        });

        stage.setScene(scene);
        stage.show();
    }

    private void sendMessage(Rectangle rect, String msg){
        TransferData data = new TransferData();
        data.setData(rect.getX() + ":" + rect.getY());
        data.setToken(msg);
        data.setUser(NICKNAME);
        Client.connections.get(0).getOut().send(data.build());
    }

    @Override
    public void clientConnectionExecute(InputStream inputStream, OutputStream outputStream, Socket socket) throws IOException {
        while (true) {
            try {
                String userMessage = inputStream.readLine();
                if (userMessage != null && !userMessage.equals("")) {
                    TransferData data = TransferData.reciveTransferData(userMessage);
                    receiveMessage(data);
                }
            } catch (SocketException e) {
                break;
            }
        }
    }

    private Rectangle initNewRectangle(){
        return initNewRectangle(0d,0d, PLAYER_BOX_WIDTH, PLAYER_BOX_HEIGHT);
    }

    private Rectangle initNewRectangle(Double x, Double y){
        return initNewRectangle(x,y, PLAYER_BOX_WIDTH, PLAYER_BOX_HEIGHT);
    }

    private Rectangle initNewRectangle(Double x, Double y, Double w, Double h){
        Rectangle rect = new Rectangle(w,h);
        rect.setFill(Color.color(Math.random(), Math.random(), Math.random()));
        rect.setX(x);
        rect.setY(y);
        return rect;
    }

    private void login(){
        TransferData data = new TransferData();
        data.setData("");
        data.setToken("LOGIN");
        data.setUser(NICKNAME);
        while (Client.connections == null || Client.connections.get(0) == null){}
        Client.connections.get(0).getOut().send(data.build());
    }

    private void receiveMessage(TransferData data){
        Platform.runLater(() -> {
            if (data.getToken() != null && data.getToken().length() > 0) {
                if (data.getToken().equals("LOGIN") && !NICKNAME.equals(data.getUser())) {
                    String[] coordinates = data.getData() != null ? data.getData().split(":") : new String[]{"0", "0"};
                    createPlayer(Double.valueOf(coordinates[0]), Double.valueOf(coordinates[1]), data.getUser());
                }
                if (data.getToken().equals("BULLET")) {
                    if (data.getData() != null && data.getData().equals("REMOVE")){
                        removeBullet(data.getUser());
                        return;
                    }
                    if (data.getData() != null && data.getData().contains("REMOVE:")){
                        removeBullet(data.getUser());
                        changePlayerColor(data.getData().split(":")[1]);
                        return;
                    }
                    String[] bulletCoordinates = data.getData() != null ? data.getData().split(":") : new String[]{"0", "0"};
                    if (bulletMap.get(data.getUser()) == null) {
                        createBullet(Double.valueOf(bulletCoordinates[0]), Double.valueOf(bulletCoordinates[1]), data.getUser());
                    } else {
                        repaintBullet(Double.valueOf(bulletCoordinates[0]), Double.valueOf(bulletCoordinates[1]), data.getUser());
                    }
                }
            } else {
                if (data.getData() != null) {
                    String[] coordinates = data.getData().split(":");
                    repaintPlayerBox(Double.valueOf(coordinates[0]), Double.valueOf(coordinates[1]), data.getUser());
                }
            }
        });
    }

    private void removeBullet(String user){
        Rectangle oldBullet = bulletMap.get(user);
        root.getChildren().remove(oldBullet);
        bulletMap.remove(user);
    }

    private void createBullet(Double x, Double y, String user){
        Rectangle newBullet = initNewRectangle(x, y, BULLET_BOX_WIDTH, BULLET_BOX_HEIGHT);
        root.getChildren().add(newBullet);
        bulletMap.put(user, newBullet);
    }

    private void createPlayer(Double x, Double y, String user){
        Rectangle newPlayer = initNewRectangle(x, y);
        root.getChildren().add(newPlayer);
        playerBoxesMap.put(user, newPlayer);
    }

    private void repaintBullet(Double x, Double y, String user){
        Rectangle bullet = bulletMap.get(user);
        if (bullet != null) {
            bullet.setX(x);
            bullet.setY(y);
        }
    }

    private void changePlayerColor(String user){
        Rectangle player = playerBoxesMap.get(user);
        player.setFill(Color.color(Math.random(), Math.random(), Math.random()));
    }

    private void repaintPlayerBox(Double x, Double y, String user){
        Rectangle rectangle = playerBoxesMap.get(user);
        if (rectangle != null) {
            rectangle.setX(x);
            rectangle.setY(y);
        }
    }

}
