package com.company;

import com.owlike.genson.Genson;
import org.eclipse.paho.client.mqttv3.*;

import javax.xml.crypto.Data;
import java.io.Serializable;
import java.util.*;

public class ESP32Measurement {
    public static HashMap<Integer, Character> translationTable = new HashMap<Integer, Character>() {
        {
            put(0, '0');
            put(1, '1');
            put(2, '2');
            put(3, '3');
            put(4, '4');
            put(5, '5');
            put(6, '6');
            put(7, '7');
            put(8, '8');
            put(9, '9');
            put(10, '.');
            put(11, ',');
            put(12, '-');
        }
    };

    String username;
    String client_id;
    MqttClient mqttClient;
    int bytesize;
    List<Byte> buffer = new ArrayList<>();
    MqttTopic publishTopic;
    ArrayList<Datapoint> datapoints = new ArrayList<>();

    public ESP32Measurement(String client_id, String username, MqttClient mqttClient, int bytesize){
        this.client_id = client_id;
        this.username = username;
        this.mqttClient = mqttClient;
        this.bytesize = bytesize;


        MqttMessage message = new MqttMessage("DATA,GO".getBytes());
        String publishTopicName = "control/response/"+client_id;
        publishTopic = mqttClient.getTopic(publishTopicName);
        MqttDeliveryToken token;
        try {
            token = publishTopic.publish(message);
            token.waitForCompletion();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public boolean fillBuffer(byte[] data){
        for(byte b: data){
            buffer.add(b);
        }
        System.out.println(buffer.size());
        if(buffer.size() == bytesize){
            MqttMessage message = new MqttMessage("DATA,OK".getBytes());
            MqttDeliveryToken token;
            try {
                token = publishTopic.publish(message);
                token.waitForCompletion();
            } catch (MqttException e) {
                e.printStackTrace();
            }
            return processBuffer();
        }
        return false;
    }


    private boolean processBuffer(){
        try {
            System.out.println("Processing buffer...");
            String bitString = toBitString(buffer.toArray(new Byte[buffer.size()]));
            System.out.println(bytesToHex(buffer.toArray(new Byte[buffer.size()])));
            System.out.println(bitString);
            int i = 0;
            while (i < bitString.length()) {
                if (bitString.charAt(i) == '0') {
                    //FulldataPoint ahead, next 21 bytes are datapoint
                    datapoints.add(getFullDataPoint(bitString.substring(i, i + 21 * 8)));
                    i += 21 * 8;
                } else {
                    //Reduced datapoint ahead, next 4 bytes are datapoint
                    datapoints.add(getReducedDataPoint(bitString.substring(i, i + 4 * 8), datapoints.get(datapoints.size() - 1)));
                    i += 4 * 8;
                }
            }
            System.out.println(toJSON());
            return true;
        }
        catch(Exception e){
            System.out.println("Error while processing data: ");
            System.out.println(e.getMessage());
        }
        return false;
    }

    Datapoint getFullDataPoint(String data){
        StringBuilder realdata = new StringBuilder();
        List<String> chars = new ArrayList<String>();
        int index = 4; //Eerste 4 bits zijn header
        while (index < data.length()) {
            chars.add(data.substring(index, Math.min(index + 4,data.length())));
            index += 4;
        }
        for(String s: chars){
            int value = Integer.parseInt(s, 2);
            if(value != 15) {
                realdata.append(translationTable.get(Integer.parseInt(s, 2)));
            }
        }
        String[] splitted = realdata.toString().split(",");
        return new Datapoint(Long.parseLong(splitted[0]), Double.parseDouble(splitted[1]), Double.parseDouble(splitted[2]), Float.parseFloat(splitted[3]), Float.parseFloat(splitted[4]));
    }

    Datapoint getReducedDataPoint(String data, Datapoint previous){
        int time_delta = Integer.parseInt(data.substring(1,3), 2);
        boolean lat_pos = data.substring(3,4).equals("0");
        double lat_delta = (double) Integer.parseInt(data.substring(4,9),2) * (lat_pos ? 1 : -1) /100000;
        boolean lon_pos = data.substring(9,10).equals("0");
        double lon_delta = (double) Integer.parseInt(data.substring(10,15),2) * (lon_pos ? 1 : -1) /100000;
        boolean speed_pos = data.substring(15,16).equals("0");
        float speed_delta = (float) Integer.parseInt(data.substring(16,23), 2) * (speed_pos ? 1 : -1) / 10;
        float vibration = (float) Integer.parseInt(data.substring(23,32),2)/10;
        return new Datapoint(time_delta, lat_delta, lon_delta, speed_delta, vibration, previous);
    }



    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(Byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public String toJSON(){

        Map<String, Object> ride = new HashMap<String, Object>(){{
            put("name", username + "-" + datapoints.get(0).timestamp);
            put("distance", getTotalDistance());
            put("avSpeed", getAvSpeed());
            put("timeStamp", datapoints.get(0).timestamp*1000);
            put("duration", (datapoints.get(datapoints.size()-1).timestamp - datapoints.get(0).timestamp)*1000);
            put("id", -1);
            put("calibration", -1);
            put("type", "meting");
        }};

        List<Map<String, Object>> measurementList = new ArrayList<>();
        for(Datapoint dp: datapoints){
            Map<String, Object> measurement = new HashMap<String, Object>(){{
                put("timeStamp", dp.getTimestamp()*1000);
                put("latitude", dp.getLat());
                put("longitude", dp.getLon());
                put("altitude", 0);
                put("accuracy","-1");
                put("measurement",dp.getVibr());
                put("lightMeasurement",0);
            }};
            measurementList.add(measurement);
        }
        ride.put("measurements", measurementList);
        Genson genson = new Genson();
        return genson.serialize(ride);
    }

    //Afstand tussen 2 coordinaten bepalen
    public double getTotalDistance(){
        Datapoint previous = datapoints.get(0);
        double totalDistance = 0;
        double earthRadius = 6378137; //meters
        for(int i = 1; i < datapoints.size(); i++){
            Datapoint current = datapoints.get(i);

            double dLat = Math.toRadians(current.lat-previous.lat);
            double dLng = Math.toRadians(current.lon-previous.lon);
            double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                    Math.cos(Math.toRadians(previous.lat)) * Math.cos(Math.toRadians(current.lat)) *
                            Math.sin(dLng/2) * Math.sin(dLng/2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
            totalDistance += earthRadius * c;
            previous = current;
        }
        return totalDistance;
    }

    public double getAvSpeed(){
        double speed = 0;
        for(Datapoint dp : datapoints){
            speed += dp.speed;
        }
        return speed/datapoints.size();
    }


    public static String toBitString(final Byte[] b) {
        final char[] bits = new char[8 * b.length];
        for(int i = 0; i < b.length; i++) {
            final byte byteval = b[i];
            int bytei = i << 3;
            int mask = 0x1;
            for(int j = 7; j >= 0; j--) {
                final int bitval = byteval & mask;
                if(bitval == 0) {
                    bits[bytei + j] = '0';
                } else {
                    bits[bytei + j] = '1';
                }
                mask <<= 1;
            }
        }
        return String.valueOf(bits);
    }



}
