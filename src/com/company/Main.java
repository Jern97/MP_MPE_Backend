package com.company;

public class Main {

    /**
     *
     * MAIN
     *
     */
    public static void main(String[] args) throws Exception {
        MqttBackend smc = MqttBackend.getInstance();
        smc.connect();
    }
}
