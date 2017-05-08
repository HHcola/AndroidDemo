package com.squareup.okhttp.internal.http;

import java.io.IOException;
import okio.Sink;

public abstract interface CacheRequest
{
  public abstract Sink body()
    throws IOException;
  
  public abstract void abort();
}


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\http\CacheRequest.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */