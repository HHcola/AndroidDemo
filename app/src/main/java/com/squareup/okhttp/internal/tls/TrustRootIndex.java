package com.squareup.okhttp.internal.tls;

import java.security.cert.X509Certificate;

public abstract interface TrustRootIndex
{
  public abstract X509Certificate findByIssuerAndSignature(X509Certificate paramX509Certificate);
}


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\tls\TrustRootIndex.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */