package com.squareup.okhttp;

import java.net.Socket;

public abstract interface Connection
{
  public abstract Route getRoute();
  
  public abstract Socket getSocket();
  
  public abstract Handshake getHandshake();
  
  public abstract Protocol getProtocol();
}


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\Connection.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */