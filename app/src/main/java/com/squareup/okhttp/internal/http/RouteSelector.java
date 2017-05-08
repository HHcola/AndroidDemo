/*     */ package com.squareup.okhttp.internal.http;
/*     */ 
/*     */ import com.squareup.okhttp.Address;
/*     */ import com.squareup.okhttp.Dns;
/*     */ import com.squareup.okhttp.HttpUrl;
/*     */ import com.squareup.okhttp.Route;
/*     */ import com.squareup.okhttp.internal.RouteDatabase;
/*     */ import java.io.IOException;
/*     */ import java.net.InetAddress;
/*     */ import java.net.InetSocketAddress;
/*     */ import java.net.Proxy;
/*     */ import java.net.Proxy.Type;
/*     */ import java.net.ProxySelector;
/*     */ import java.net.SocketAddress;
/*     */ import java.net.SocketException;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collections;
/*     */ import java.util.List;
/*     */ import java.util.NoSuchElementException;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public final class RouteSelector
/*     */ {
/*     */   private final Address address;
/*     */   private final RouteDatabase routeDatabase;
/*     */   private Proxy lastProxy;
/*     */   private InetSocketAddress lastInetSocketAddress;
/*  47 */   private List<Proxy> proxies = Collections.emptyList();
/*     */   
/*     */   private int nextProxyIndex;
/*     */   
/*  51 */   private List<InetSocketAddress> inetSocketAddresses = Collections.emptyList();
/*     */   
/*     */   private int nextInetSocketAddressIndex;
/*     */   
/*  55 */   private final List<Route> postponedRoutes = new ArrayList();
/*     */   
/*     */   public RouteSelector(Address address, RouteDatabase routeDatabase) {
/*  58 */     this.address = address;
/*  59 */     this.routeDatabase = routeDatabase;
/*     */     
/*  61 */     resetNextProxy(address.url(), address.getProxy());
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public boolean hasNext()
/*     */   {
/*  71 */     return (hasNextInetSocketAddress()) || (hasNextProxy()) || (hasNextPostponed());
/*     */   }
/*     */   
/*     */   public Route next() throws IOException
/*     */   {
/*  76 */     if (!hasNextInetSocketAddress()) {
/*  77 */       if (!hasNextProxy()) {
/*  78 */         if (!hasNextPostponed()) {
/*  79 */           throw new NoSuchElementException();
/*     */         }
/*  81 */         return nextPostponed();
/*     */       }
/*  83 */       this.lastProxy = nextProxy();
/*     */     }
/*  85 */     this.lastInetSocketAddress = nextInetSocketAddress();
/*     */     
/*  87 */     Route route = new Route(this.address, this.lastProxy, this.lastInetSocketAddress);
/*  88 */     if (this.routeDatabase.shouldPostpone(route)) {
/*  89 */       this.postponedRoutes.add(route);
/*     */       
/*  91 */       return next();
/*     */     }
/*     */     
/*  94 */     return route;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public void connectFailed(Route failedRoute, IOException failure)
/*     */   {
/* 102 */     if ((failedRoute.getProxy().type() != Proxy.Type.DIRECT) && (this.address.getProxySelector() != null))
/*     */     {
/* 104 */       this.address.getProxySelector().connectFailed(this.address
/* 105 */         .url().uri(), failedRoute.getProxy().address(), failure);
/*     */     }
/*     */     
/* 108 */     this.routeDatabase.failed(failedRoute);
/*     */   }
/*     */   
/*     */   private void resetNextProxy(HttpUrl url, Proxy proxy)
/*     */   {
/* 113 */     if (proxy != null)
/*     */     {
/* 115 */       this.proxies = Collections.singletonList(proxy);
/*     */     }
/*     */     else
/*     */     {
/* 119 */       this.proxies = new ArrayList();
/* 120 */       List<Proxy> selectedProxies = this.address.getProxySelector().select(url.uri());
/* 121 */       if (selectedProxies != null) { this.proxies.addAll(selectedProxies);
/*     */       }
/* 123 */       this.proxies.removeAll(Collections.singleton(Proxy.NO_PROXY));
/* 124 */       this.proxies.add(Proxy.NO_PROXY);
/*     */     }
/* 126 */     this.nextProxyIndex = 0;
/*     */   }
/*     */   
/*     */   private boolean hasNextProxy()
/*     */   {
/* 131 */     return this.nextProxyIndex < this.proxies.size();
/*     */   }
/*     */   
/*     */   private Proxy nextProxy() throws IOException
/*     */   {
/* 136 */     if (!hasNextProxy()) {
/* 137 */       throw new SocketException("No route to " + this.address.getUriHost() + "; exhausted proxy configurations: " + this.proxies);
/*     */     }
/*     */     
/* 140 */     Proxy result = (Proxy)this.proxies.get(this.nextProxyIndex++);
/* 141 */     resetNextInetSocketAddress(result);
/* 142 */     return result;
/*     */   }
/*     */   
/*     */   private void resetNextInetSocketAddress(Proxy proxy)
/*     */     throws IOException
/*     */   {
/* 148 */     this.inetSocketAddresses = new ArrayList();
/*     */     int socketPort;
/*     */     String socketHost;
/*     */     int socketPort;
/* 152 */     if ((proxy.type() == Proxy.Type.DIRECT) || (proxy.type() == Proxy.Type.SOCKS)) {
/* 153 */       String socketHost = this.address.getUriHost();
/* 154 */       socketPort = this.address.getUriPort();
/*     */     } else {
/* 156 */       SocketAddress proxyAddress = proxy.address();
/* 157 */       if (!(proxyAddress instanceof InetSocketAddress))
/*     */       {
/* 159 */         throw new IllegalArgumentException("Proxy.address() is not an InetSocketAddress: " + proxyAddress.getClass());
/*     */       }
/* 161 */       InetSocketAddress proxySocketAddress = (InetSocketAddress)proxyAddress;
/* 162 */       socketHost = getHostString(proxySocketAddress);
/* 163 */       socketPort = proxySocketAddress.getPort();
/*     */     }
/*     */     
/* 166 */     if ((socketPort < 1) || (socketPort > 65535)) {
/* 167 */       throw new SocketException("No route to " + socketHost + ":" + socketPort + "; port is out of range");
/*     */     }
/*     */     
/*     */ 
/* 171 */     if (proxy.type() == Proxy.Type.SOCKS) {
/* 172 */       this.inetSocketAddresses.add(InetSocketAddress.createUnresolved(socketHost, socketPort));
/*     */     }
/*     */     else {
/* 175 */       List<InetAddress> addresses = this.address.getDns().lookup(socketHost);
/* 176 */       int i = 0; for (int size = addresses.size(); i < size; i++) {
/* 177 */         InetAddress inetAddress = (InetAddress)addresses.get(i);
/* 178 */         this.inetSocketAddresses.add(new InetSocketAddress(inetAddress, socketPort));
/*     */       }
/*     */     }
/*     */     
/* 182 */     this.nextInetSocketAddressIndex = 0;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   static String getHostString(InetSocketAddress socketAddress)
/*     */   {
/* 191 */     InetAddress address = socketAddress.getAddress();
/* 192 */     if (address == null)
/*     */     {
/*     */ 
/*     */ 
/* 196 */       return socketAddress.getHostName();
/*     */     }
/*     */     
/*     */ 
/* 200 */     return address.getHostAddress();
/*     */   }
/*     */   
/*     */   private boolean hasNextInetSocketAddress()
/*     */   {
/* 205 */     return this.nextInetSocketAddressIndex < this.inetSocketAddresses.size();
/*     */   }
/*     */   
/*     */   private InetSocketAddress nextInetSocketAddress() throws IOException
/*     */   {
/* 210 */     if (!hasNextInetSocketAddress()) {
/* 211 */       throw new SocketException("No route to " + this.address.getUriHost() + "; exhausted inet socket addresses: " + this.inetSocketAddresses);
/*     */     }
/*     */     
/* 214 */     return (InetSocketAddress)this.inetSocketAddresses.get(this.nextInetSocketAddressIndex++);
/*     */   }
/*     */   
/*     */   private boolean hasNextPostponed()
/*     */   {
/* 219 */     return !this.postponedRoutes.isEmpty();
/*     */   }
/*     */   
/*     */   private Route nextPostponed()
/*     */   {
/* 224 */     return (Route)this.postponedRoutes.remove(0);
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\http\RouteSelector.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */