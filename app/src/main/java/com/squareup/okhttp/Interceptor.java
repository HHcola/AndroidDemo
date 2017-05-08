package com.squareup.okhttp;

import java.io.IOException;

public abstract interface Interceptor
{
  public abstract Response intercept(Chain paramChain)
    throws IOException;
  
  public static abstract interface Chain
  {
    public abstract Request request();
    
    public abstract Response proceed(Request paramRequest)
      throws IOException;
    
    public abstract Connection connection();
  }
}


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\Interceptor.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */