import socket
import threading
import hashlib
from MPE_Parser import parse_file

TCP_IP = '192.168.43.132'
TCP_PORT = 8080
BUFFER_SIZE = 1024  # Normally 1024, but we want fast response


class ThreadedServer(object):
    def __init__(self, host, port):
        self.host = host
        self.port = port
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
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
        f = open("data/" + file_name, "w")
        while 1:
            data = conn.recv(BUFFER_SIZE)
            if data:
                print(data)
                hash = hashlib.md5(data)
                data = data.decode("UTF-8")
                if (data == 'END'):
                    print("done")
                    conn.send(bytes('OK', 'utf-8'))
                    f.close()
                    parse_file(file_name)
                    return
                f.write(data)
                conn.send(hash.hexdigest().encode('utf-8'))



ThreadedServer(TCP_IP, TCP_PORT).listen()
