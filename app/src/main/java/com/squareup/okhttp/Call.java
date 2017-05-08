/*     */ package com.squareup.okhttp;
/*     */ 
/*     */ import com.squareup.okhttp.internal.Internal;
/*     */ import com.squareup.okhttp.internal.NamedRunnable;
/*     */ import com.squareup.okhttp.internal.http.HttpEngine;
/*     */ import com.squareup.okhttp.internal.http.RequestException;
/*     */ import com.squareup.okhttp.internal.http.RouteException;
/*     */ import com.squareup.okhttp.internal.http.StreamAllocation;
/*     */ import java.io.IOException;
/*     */ import java.net.ProtocolException;
/*     */ import java.util.List;
/*     */ import java.util.logging.Level;
/*     */ import java.util.logging.Logger;
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
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class Call
/*     */ {
/*     */   private final OkHttpClient client;
/*     */   private boolean executed;
/*     */   volatile boolean canceled;
/*     */   Request originalRequest;
/*     */   HttpEngine engine;
/*     */   
/*     */   protected Call(OkHttpClient client, Request originalRequest)
/*     */   {
/*  49 */     this.client = client.copyWithDefaults();
/*  50 */     this.originalRequest = originalRequest;
/*     */   }
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
/*     */   public Response execute()
/*     */     throws IOException
/*     */   {
/*  74 */     synchronized (this) {
/*  75 */       if (this.executed) throw new IllegalStateException("Already Executed");
/*  76 */       this.executed = true;
/*     */     }
/*     */     try {
/*  79 */       this.client.getDispatcher().executed(this);
/*  80 */       Response result = getResponseWithInterceptorChain(false);
/*  81 */       if (result == null) throw new IOException("Canceled");
/*  82 */       return result;
/*     */     } finally {
/*  84 */       this.client.getDispatcher().finished(this);
/*     */     }
/*     */   }
/*     */   
/*     */   Object tag() {
/*  89 */     return this.originalRequest.tag();
/*     */   }
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
/*     */   public void enqueue(Callback responseCallback)
/*     */   {
/* 106 */     enqueue(responseCallback, false);
/*     */   }
/*     */   
/*     */   void enqueue(Callback responseCallback, boolean forWebSocket) {
/* 110 */     synchronized (this) {
/* 111 */       if (this.executed) throw new IllegalStateException("Already Executed");
/* 112 */       this.executed = true;
/*     */     }
/* 114 */     this.client.getDispatcher().enqueue(new AsyncCall(responseCallback, forWebSocket, null));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public void cancel()
/*     */   {
/* 122 */     this.canceled = true;
/* 123 */     if (this.engine != null) { this.engine.cancel();
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public synchronized boolean isExecuted()
/*     */   {
/* 131 */     return this.executed;
/*     */   }
/*     */   
/*     */   public boolean isCanceled() {
/* 135 */     return this.canceled;
/*     */   }
/*     */   
/*     */   final class AsyncCall extends NamedRunnable {
/*     */     private final Callback responseCallback;
/*     */     private final boolean forWebSocket;
/*     */     
/*     */     private AsyncCall(Callback responseCallback, boolean forWebSocket) {
/* 143 */       super(new Object[] { Call.this.originalRequest.urlString() });
/* 144 */       this.responseCallback = responseCallback;
/* 145 */       this.forWebSocket = forWebSocket;
/*     */     }
/*     */     
/*     */     String host() {
/* 149 */       return Call.this.originalRequest.httpUrl().host();
/*     */     }
/*     */     
/*     */     Request request() {
/* 153 */       return Call.this.originalRequest;
/*     */     }
/*     */     
/*     */     Object tag() {
/* 157 */       return Call.this.originalRequest.tag();
/*     */     }
/*     */     
/*     */     void cancel() {
/* 161 */       Call.this.cancel();
/*     */     }
/*     */     
/*     */     Call get() {
/* 165 */       return Call.this;
/*     */     }
/*     */     
/*     */     protected void execute() {
/* 169 */       boolean signalledCallback = false;
/*     */       try {
/* 171 */         Response response = Call.this.getResponseWithInterceptorChain(this.forWebSocket);
/* 172 */         if (Call.this.canceled) {
/* 173 */           signalledCallback = true;
/* 174 */           this.responseCallback.onFailure(Call.this.originalRequest, new IOException("Canceled"));
/*     */         } else {
/* 176 */           signalledCallback = true;
/* 177 */           this.responseCallback.onResponse(response);
/*     */         }
/*     */       } catch (IOException e) {
/* 180 */         if (signalledCallback)
/*     */         {
/* 182 */           Internal.logger.log(Level.INFO, "Callback failure for " + Call.this.toLoggableString(), e);
/*     */         } else {
/* 184 */           Request request = Call.this.engine == null ? Call.this.originalRequest : Call.this.engine.getRequest();
/* 185 */           this.responseCallback.onFailure(request, e);
/*     */         }
/*     */       } finally {
/* 188 */         Call.this.client.getDispatcher().finished(this);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private String toLoggableString()
/*     */   {
/* 198 */     String string = this.canceled ? "canceled call" : "call";
/* 199 */     HttpUrl redactedUrl = this.originalRequest.httpUrl().resolve("/...");
/* 200 */     return string + " to " + redactedUrl;
/*     */   }
/*     */   
/*     */   private Response getResponseWithInterceptorChain(boolean forWebSocket) throws IOException {
/* 204 */     Interceptor.Chain chain = new ApplicationInterceptorChain(0, this.originalRequest, forWebSocket);
/* 205 */     return chain.proceed(this.originalRequest);
/*     */   }
/*     */   
/*     */   class ApplicationInterceptorChain implements Interceptor.Chain {
/*     */     private final int index;
/*     */     private final Request request;
/*     */     private final boolean forWebSocket;
/*     */     
/*     */     ApplicationInterceptorChain(int index, Request request, boolean forWebSocket) {
/* 214 */       this.index = index;
/* 215 */       this.request = request;
/* 216 */       this.forWebSocket = forWebSocket;
/*     */     }
/*     */     
/*     */     public Connection connection() {
/* 220 */       return null;
/*     */     }
/*     */     
/*     */     public Request request() {
/* 224 */       return this.request;
/*     */     }
/*     */     
/*     */     public Response proceed(Request request) throws IOException
/*     */     {
/* 229 */       if (this.index < Call.this.client.interceptors().size()) {
/* 230 */         Interceptor.Chain chain = new ApplicationInterceptorChain(Call.this, this.index + 1, request, this.forWebSocket);
/* 231 */         Interceptor interceptor = (Interceptor)Call.this.client.interceptors().get(this.index);
/* 232 */         Response interceptedResponse = interceptor.intercept(chain);
/*     */         
/* 234 */         if (interceptedResponse == null) {
/* 235 */           throw new NullPointerException("application interceptor " + interceptor + " returned null");
/*     */         }
/*     */         
/*     */ 
/* 239 */         return interceptedResponse;
/*     */       }
/*     */       
/*     */ 
/* 243 */       return Call.this.getResponse(request, this.forWebSocket);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   Response getResponse(Request request, boolean forWebSocket)
/*     */     throws IOException
/*     */   {
/* 253 */     RequestBody body = request.body();
/* 254 */     if (body != null) {
/* 255 */       Request.Builder requestBuilder = request.newBuilder();
/*     */       
/* 257 */       MediaType contentType = body.contentType();
/* 258 */       if (contentType != null) {
/* 259 */         requestBuilder.header("Content-Type", contentType.toString());
/*     */       }
/*     */       
/* 262 */       long contentLength = body.contentLength();
/* 263 */       if (contentLength != -1L) {
/* 264 */         requestBuilder.header("Content-Length", Long.toString(contentLength));
/* 265 */         requestBuilder.removeHeader("Transfer-Encoding");
/*     */       } else {
/* 267 */         requestBuilder.header("Transfer-Encoding", "chunked");
/* 268 */         requestBuilder.removeHeader("Content-Length");
/*     */       }
/*     */       
/* 271 */       request = requestBuilder.build();
/*     */     }
/*     */     
/*     */ 
/* 275 */     this.engine = new HttpEngine(this.client, request, false, false, forWebSocket, null, null, null);
/*     */     
/* 277 */     int followUpCount = 0;
/*     */     for (;;) {
/* 279 */       if (this.canceled) {
/* 280 */         this.engine.releaseStreamAllocation();
/* 281 */         throw new IOException("Canceled");
/*     */       }
/*     */       
/* 284 */       boolean releaseConnection = true;
/*     */       try {
/* 286 */         this.engine.sendRequest();
/* 287 */         this.engine.readResponse();
/* 288 */         releaseConnection = false;
/*     */       } catch (RequestException e) {
/*     */         StreamAllocation streamAllocation;
/* 291 */         throw e.getCause();
/*     */       }
/*     */       catch (RouteException e) {
/* 294 */         HttpEngine retryEngine = this.engine.recover(e);
/* 295 */         if (retryEngine != null) {
/* 296 */           releaseConnection = false;
/* 297 */           this.engine = retryEngine;
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
/* 315 */           if (!releaseConnection) continue;
/* 316 */           StreamAllocation streamAllocation = this.engine.close();
/* 317 */           streamAllocation.release();
/* 318 */           continue;
/*     */         }
/* 301 */         throw e.getLastConnectException();
/*     */       }
/*     */       catch (IOException e) {
/* 304 */         HttpEngine retryEngine = this.engine.recover(e, null);
/* 305 */         if (retryEngine != null) {
/* 306 */           releaseConnection = false;
/* 307 */           this.engine = retryEngine;
/*     */           
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 315 */           if (!releaseConnection) continue;
/* 316 */           StreamAllocation streamAllocation = this.engine.close();
/* 317 */           streamAllocation.release();
/* 318 */           continue;
/*     */         }
/* 312 */         throw e;
/*     */       }
/*     */       finally {
/* 315 */         if (releaseConnection) {
/* 316 */           StreamAllocation streamAllocation = this.engine.close();
/* 317 */           streamAllocation.release();
/*     */         }
/*     */       }
/*     */       
/* 321 */       Response response = this.engine.getResponse();
/* 322 */       Request followUp = this.engine.followUpRequest();
/*     */       
/* 324 */       if (followUp == null) {
/* 325 */         if (!forWebSocket) {
/* 326 */           this.engine.releaseStreamAllocation();
/*     */         }
/* 328 */         return response;
/*     */       }
/*     */       
/* 331 */       StreamAllocation streamAllocation = this.engine.close();
/*     */       
/* 333 */       followUpCount++; if (followUpCount > 20) {
/* 334 */         streamAllocation.release();
/* 335 */         throw new ProtocolException("Too many follow-up requests: " + followUpCount);
/*     */       }
/*     */       
/* 338 */       if (!this.engine.sameConnection(followUp.httpUrl())) {
/* 339 */         streamAllocation.release();
/* 340 */         streamAllocation = null;
/*     */       }
/*     */       
/* 343 */       request = followUp;
/* 344 */       this.engine = new HttpEngine(this.client, request, false, false, forWebSocket, streamAllocation, null, response);
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\Call.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */