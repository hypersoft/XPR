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

  private int reader, writer, self;

  private void onConnect() {
    InputStream oreader; OutputStream owriter;
    try {
      oreader = super.getInputStream();
      owriter = super.getOutputStream();
    } catch (IOException e) { throw new Fault(e); }
    self = Stream.add(this);
    reader = Stream.add(oreader);
    writer = Stream.add(owriter);
  }

  @Override
  public InputStream getInputStream() {
    return Stream.get(reader);
  }

  @Override
  public OutputStream getOutputStream() {
    return Stream.get(writer);
  }

  public int getReader() {
    return reader;
  }

  public int getWriter() {
    return writer;
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
