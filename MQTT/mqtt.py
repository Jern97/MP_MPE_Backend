import time
import paho.mqtt.client as paho
import ssl
import os

#define callbacks
def on_message(client, userdata, message):
  print("received message =",str(message.payload.decode("utf-8")))

def on_log(client, userdata, level, buf):
  print("log: ",buf)

def on_connect(client, userdata, flags, rc):
  print("subscribing ")
  client.subscribe("control/jeroen/request")


client=paho.Client()
client.on_message=on_message
#client.on_log=on_log
client.on_connect=on_connect
client.username_pw_set("jeroen", "jeroen")
print("connecting to broker")
cert = os.path.dirname(os.path.realpath(__file__)) + "/ca.crt"
print(cert)
client.tls_set(cert, tls_version=ssl.PROTOCOL_TLSv1_2)
client.tls_insecure_set(True)
client.connect("10.55.22.105", 8883, 60)

##start loop to process received messages
client.loop_forever()



