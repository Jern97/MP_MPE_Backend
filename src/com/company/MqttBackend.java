package com.company;


import org.eclipse.paho.client.mqttv3.*;

import java.util.HashMap;

public class MqttBackend implements MqttCallback {

    private static MqttBackend instance = new MqttBackend();

    private MqttBackend(){}

    private MqttClient mqttClient;
    private MqttConnectOptions connOpt;

    private HashMap<String, ESP32Measurement> measurementHashMap = new HashMap<>();

    final private String BROKER_URL = "tcp://127.0.0.1:1883";
    final private String ID = "backend";
    final private String USERNAME = "backend";
    final private String PASSWORD = "backend";

    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String topic_parts[] = topic.split("/");
        if(topic_parts[0].equals("control")){
            String message_parts[] = (new String(message.getPayload()).split(","));
            if(message_parts[0].equals("DATA")){
                //Client wants to send measurements
                int bytesize = Integer.parseInt(message_parts[1]);
                String username = message_parts[2];
                String password = message_parts[3];
                String client_id = topic_parts[2];

                measurementHashMap.put(client_id, new ESP32Measurement(client_id, username, mqttClient,  bytesize));
            }

        }
        if(topic_parts[0].equals("data")){
            String client_id = topic_parts[1];
            measurementHashMap.get(client_id).fillBuffer(message.getPayload());
        }
    }

    public void connect() throws Exception{
        // setup MQTT Client
        String clientID = ID;
        connOpt = new MqttConnectOptions();

        connOpt.setCleanSession(true);
        connOpt.setConnectionTimeout(60);
        connOpt.setKeepAliveInterval(30);
        connOpt.setUserName(USERNAME);
        connOpt.setPassword(PASSWORD.toCharArray());
        // Connect to Broker
        try {
            mqttClient = new MqttClient(BROKER_URL, clientID);
            mqttClient.setCallback(this);
            mqttClient.connect(connOpt);
        } catch (MqttException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        System.out.println("Connected to " + BROKER_URL);

        String subscribeTopic1 = "control/request/#";
        String subscribeTopic2 = "data/#";



        try {
            int subQoS = 2;
            mqttClient.subscribe(subscribeTopic1, subQoS);
            mqttClient.subscribe(subscribeTopic2, subQoS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // disconnect
        try {
            Thread.sleep(Long.MAX_VALUE);
            mqttClient.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeESP32Measurement(String username){
        measurementHashMap.remove(username);
    }

    static MqttBackend getInstance(){
        return instance;
    }

    public void connectionLost(Throwable t) {
        System.out.println("Connection lost!");
        try {
            connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deliveryComplete(IMqttDeliveryToken token) {
        //System.out.println("Pub complete" + new String(token.getMessage().getPayload()));
    }
}