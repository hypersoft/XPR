package XPR.IO.Network;

// Good data: http://tutorials.jenkov.com/java-networking/index.html

import XPR.Fault;
import XPR.IO.Stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Socket extends java.net.Socket {

  InputStream oreader; OutputStream owriter;

  int openSocket(String host, int port) {
    Socket socket;
    try {
      socket = new Socket(host, port);
      return Stream.add(socket);
    } catch (IOException e) { throw new Fault(e); }
  }

  private void onConnect() {
    try {
      oreader = super.getInputStream();
      owriter = super.getOutputStream();
    } catch (IOException e) { throw new Fault(e); }
  }

  @Override
  public InputStream getInputStream() {
    return oreader;
  }

  @Override
  public OutputStream getOutputStream() {
    return owriter;
  }

  public Socket(String host, int port) throws UnknownHostException, IOException
  {
    super(host, port);
    onConnect();
  }

  public Socket(InetAddress address, int port) throws IOException {
    super(address, port);
    onConnect();
  }

  public Socket(String host, int port, InetAddress localAddr,
    int localPort) throws IOException
  {
    super(host, port, localAddr, localPort);
    onConnect();
  }

  public Socket(InetAddress address, int port, InetAddress localAddr,
    int localPort) throws IOException
  {
    super(address, port, localAddr, localPort);
    onConnect();
  }

}
