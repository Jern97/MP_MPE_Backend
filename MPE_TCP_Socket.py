import socket
import threading
from MPE_Parser import parse_file
import hashlib

TCP_IP = '192.168.0.133'
TCP_PORT = 8080
BUFFER_SIZE = 1024  # Normally 1024, but we want fast response


class ThreadedServer(object):
    def __init__(self, host, port):
        self.host = host
        self.port = port
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        self.sock.bind((TCP_IP, TCP_PORT))

    def listen(self):
        self.sock.listen(5)
        while 1:
            conn, addr = self.sock.accept()
            print('Connection address: ', addr)
            conn.settimeout(10)
            threading.Thread(target=self.listen_to_conn, args=(self, conn, addr)).start()

    @staticmethod
    def listen_to_conn(self, conn, addr):
        while 1:
            try:
                data = conn.recv(BUFFER_SIZE)
                if data:
                    print(data)
                    data = data.decode("UTF-8")
                    if (data[:5] == "file:"):
                        conn.send(bytes('OK', 'utf-8'))
                        self.receive_file(self, conn, data[6:])
                    else:
                        conn.send(bytes('ERR', 'utf-8'))
                else:
                    raise error('Client disconnected')
            except Exception as e:
                print(e)
                conn.close()
                print('Connection closed: ', addr)
                return 0

    @staticmethod
    def receive_file(self, conn, file_name):
        f = open('data/' + file_name, "w")
        while 1:
            data = conn.recv(BUFFER_SIZE)
            if data:
                hash = hashlib.md5(data)
                data = data.decode("UTF-8")
                if (data == 'END'):
                    print("done")
                    conn.send(bytes('OK', 'utf-8'))
                    f.close()
                    parse_file(file_name, True);
                    return
                conn.send(bytes(hash.hexdigest(), "utf-8"))
                #print(hash.hexdigest())
                print(data)
                f.write(data)



ThreadedServer(TCP_IP, TCP_PORT).listen()
