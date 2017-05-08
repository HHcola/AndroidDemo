/*     */ package com.squareup.okhttp;
/*     */ 
/*     */ import com.squareup.okhttp.internal.http.OkHeaders;
/*     */ import java.util.Collections;
/*     */ import java.util.List;
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
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public final class Response
/*     */ {
/*     */   private final Request request;
/*     */   private final Protocol protocol;
/*     */   private final int code;
/*     */   private final String message;
/*     */   private final Handshake handshake;
/*     */   private final Headers headers;
/*     */   private final ResponseBody body;
/*     */   private Response networkResponse;
/*     */   private Response cacheResponse;
/*     */   private final Response priorResponse;
/*     */   private volatile CacheControl cacheControl;
/*     */   
/*     */   private Response(Builder builder)
/*     */   {
/*  51 */     this.request = builder.request;
/*  52 */     this.protocol = builder.protocol;
/*  53 */     this.code = builder.code;
/*  54 */     this.message = builder.message;
/*  55 */     this.handshake = builder.handshake;
/*  56 */     this.headers = builder.headers.build();
/*  57 */     this.body = builder.body;
/*  58 */     this.networkResponse = builder.networkResponse;
/*  59 */     this.cacheResponse = builder.cacheResponse;
/*  60 */     this.priorResponse = builder.priorResponse;
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
/*     */   public Request request()
/*     */   {
/*  75 */     return this.request;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public Protocol protocol()
/*     */   {
/*  83 */     return this.protocol;
/*     */   }
/*     */   
/*     */   public int code()
/*     */   {
/*  88 */     return this.code;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public boolean isSuccessful()
/*     */   {
/*  96 */     return (this.code >= 200) && (this.code < 300);
/*     */   }
/*     */   
/*     */   public String message()
/*     */   {
/* 101 */     return this.message;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public Handshake handshake()
/*     */   {
/* 109 */     return this.handshake;
/*     */   }
/*     */   
/*     */   public List<String> headers(String name) {
/* 113 */     return this.headers.values(name);
/*     */   }
/*     */   
/*     */   public String header(String name) {
/* 117 */     return header(name, null);
/*     */   }
/*     */   
/*     */   public String header(String name, String defaultValue) {
/* 121 */     String result = this.headers.get(name);
/* 122 */     return result != null ? result : defaultValue;
/*     */   }
/*     */   
/*     */   public Headers headers() {
/* 126 */     return this.headers;
/*     */   }
/*     */   
/*     */   public ResponseBody body() {
/* 130 */     return this.body;
/*     */   }
/*     */   
/*     */   public Builder newBuilder() {
/* 134 */     return new Builder(this, null);
/*     */   }
/*     */   
/*     */   public boolean isRedirect()
/*     */   {
/* 139 */     switch (this.code) {
/*     */     case 300: 
/*     */     case 301: 
/*     */     case 302: 
/*     */     case 303: 
/*     */     case 307: 
/*     */     case 308: 
/* 146 */       return true;
/*     */     }
/* 148 */     return false;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Response networkResponse()
/*     */   {
/* 158 */     return this.networkResponse;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Response cacheResponse()
/*     */   {
/* 168 */     return this.cacheResponse;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Response priorResponse()
/*     */   {
/* 178 */     return this.priorResponse;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public List<Challenge> challenges()
/*     */   {
/*     */     String responseField;
/*     */     
/*     */ 
/*     */ 
/* 190 */     if (this.code == 401) {
/* 191 */       responseField = "WWW-Authenticate"; } else { String responseField;
/* 192 */       if (this.code == 407) {
/* 193 */         responseField = "Proxy-Authenticate";
/*     */       } else
/* 195 */         return Collections.emptyList(); }
/*     */     String responseField;
/* 197 */     return OkHeaders.parseChallenges(headers(), responseField);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public CacheControl cacheControl()
/*     */   {
/* 205 */     CacheControl result = this.cacheControl;
/* 206 */     return result != null ? result : (this.cacheControl = CacheControl.parse(this.headers));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public String toString()
/*     */   {
/* 217 */     return "Response{protocol=" + this.protocol + ", code=" + this.code + ", message=" + this.message + ", url=" + this.request.urlString() + '}';
/*     */   }
/*     */   
/*     */   public static class Builder
/*     */   {
/*     */     private Request request;
/*     */     private Protocol protocol;
/* 224 */     private int code = -1;
/*     */     private String message;
/*     */     private Handshake handshake;
/*     */     private Headers.Builder headers;
/*     */     private ResponseBody body;
/*     */     private Response networkResponse;
/*     */     private Response cacheResponse;
/*     */     private Response priorResponse;
/*     */     
/*     */     public Builder() {
/* 234 */       this.headers = new Headers.Builder();
/*     */     }
/*     */     
/*     */     private Builder(Response response) {
/* 238 */       this.request = response.request;
/* 239 */       this.protocol = response.protocol;
/* 240 */       this.code = response.code;
/* 241 */       this.message = response.message;
/* 242 */       this.handshake = response.handshake;
/* 243 */       this.headers = response.headers.newBuilder();
/* 244 */       this.body = response.body;
/* 245 */       this.networkResponse = response.networkResponse;
/* 246 */       this.cacheResponse = response.cacheResponse;
/* 247 */       this.priorResponse = response.priorResponse;
/*     */     }
/*     */     
/*     */     public Builder request(Request request) {
/* 251 */       this.request = request;
/* 252 */       return this;
/*     */     }
/*     */     
/*     */     public Builder protocol(Protocol protocol) {
/* 256 */       this.protocol = protocol;
/* 257 */       return this;
/*     */     }
/*     */     
/*     */     public Builder code(int code) {
/* 261 */       this.code = code;
/* 262 */       return this;
/*     */     }
/*     */     
/*     */     public Builder message(String message) {
/* 266 */       this.message = message;
/* 267 */       return this;
/*     */     }
/*     */     
/*     */     public Builder handshake(Handshake handshake) {
/* 271 */       this.handshake = handshake;
/* 272 */       return this;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */     public Builder header(String name, String value)
/*     */     {
/* 280 */       this.headers.set(name, value);
/* 281 */       return this;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */     public Builder addHeader(String name, String value)
/*     */     {
/* 289 */       this.headers.add(name, value);
/* 290 */       return this;
/*     */     }
/*     */     
/*     */     public Builder removeHeader(String name) {
/* 294 */       this.headers.removeAll(name);
/* 295 */       return this;
/*     */     }
/*     */     
/*     */     public Builder headers(Headers headers)
/*     */     {
/* 300 */       this.headers = headers.newBuilder();
/* 301 */       return this;
/*     */     }
/*     */     
/*     */     public Builder body(ResponseBody body) {
/* 305 */       this.body = body;
/* 306 */       return this;
/*     */     }
/*     */     
/*     */     public Builder networkResponse(Response networkResponse) {
/* 310 */       if (networkResponse != null) checkSupportResponse("networkResponse", networkResponse);
/* 311 */       this.networkResponse = networkResponse;
/* 312 */       return this;
/*     */     }
/*     */     
/*     */     public Builder cacheResponse(Response cacheResponse) {
/* 316 */       if (cacheResponse != null) checkSupportResponse("cacheResponse", cacheResponse);
/* 317 */       this.cacheResponse = cacheResponse;
/* 318 */       return this;
/*     */     }
/*     */     
/*     */     private void checkSupportResponse(String name, Response response) {
/* 322 */       if (response.body != null)
/* 323 */         throw new IllegalArgumentException(name + ".body != null");
/* 324 */       if (response.networkResponse != null)
/* 325 */         throw new IllegalArgumentException(name + ".networkResponse != null");
/* 326 */       if (response.cacheResponse != null)
/* 327 */         throw new IllegalArgumentException(name + ".cacheResponse != null");
/* 328 */       if (response.priorResponse != null) {
/* 329 */         throw new IllegalArgumentException(name + ".priorResponse != null");
/*     */       }
/*     */     }
/*     */     
/*     */     public Builder priorResponse(Response priorResponse) {
/* 334 */       if (priorResponse != null) checkPriorResponse(priorResponse);
/* 335 */       this.priorResponse = priorResponse;
/* 336 */       return this;
/*     */     }
/*     */     
/*     */     private void checkPriorResponse(Response response) {
/* 340 */       if (response.body != null) {
/* 341 */         throw new IllegalArgumentException("priorResponse.body != null");
/*     */       }
/*     */     }
/*     */     
/*     */     public Response build() {
/* 346 */       if (this.request == null) throw new IllegalStateException("request == null");
/* 347 */       if (this.protocol == null) throw new IllegalStateException("protocol == null");
/* 348 */       if (this.code < 0) throw new IllegalStateException("code < 0: " + this.code);
/* 349 */       return new Response(this, null);
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\Response.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */