/*     */ package com.squareup.okhttp;
/*     */ 
/*     */ import com.squareup.okhttp.internal.http.HttpMethod;
/*     */ import java.io.IOException;
/*     */ import java.net.URI;
/*     */ import java.net.URL;
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
/*     */ public final class Request
/*     */ {
/*     */   private final HttpUrl url;
/*     */   private final String method;
/*     */   private final Headers headers;
/*     */   private final RequestBody body;
/*     */   private final Object tag;
/*     */   private volatile URL javaNetUrl;
/*     */   private volatile URI javaNetUri;
/*     */   private volatile CacheControl cacheControl;
/*     */   
/*     */   private Request(Builder builder)
/*     */   {
/*  40 */     this.url = builder.url;
/*  41 */     this.method = builder.method;
/*  42 */     this.headers = builder.headers.build();
/*  43 */     this.body = builder.body;
/*  44 */     this.tag = (builder.tag != null ? builder.tag : this);
/*     */   }
/*     */   
/*     */   public HttpUrl httpUrl() {
/*  48 */     return this.url;
/*     */   }
/*     */   
/*     */   public URL url() {
/*  52 */     URL result = this.javaNetUrl;
/*  53 */     return result != null ? result : (this.javaNetUrl = this.url.url());
/*     */   }
/*     */   
/*     */   public URI uri() throws IOException {
/*     */     try {
/*  58 */       URI result = this.javaNetUri;
/*  59 */       return result != null ? result : (this.javaNetUri = this.url.uri());
/*     */     } catch (IllegalStateException e) {
/*  61 */       throw new IOException(e.getMessage());
/*     */     }
/*     */   }
/*     */   
/*     */   public String urlString() {
/*  66 */     return this.url.toString();
/*     */   }
/*     */   
/*     */   public String method() {
/*  70 */     return this.method;
/*     */   }
/*     */   
/*     */   public Headers headers() {
/*  74 */     return this.headers;
/*     */   }
/*     */   
/*     */   public String header(String name) {
/*  78 */     return this.headers.get(name);
/*     */   }
/*     */   
/*     */   public List<String> headers(String name) {
/*  82 */     return this.headers.values(name);
/*     */   }
/*     */   
/*     */   public RequestBody body() {
/*  86 */     return this.body;
/*     */   }
/*     */   
/*     */   public Object tag() {
/*  90 */     return this.tag;
/*     */   }
/*     */   
/*     */   public Builder newBuilder() {
/*  94 */     return new Builder(this, null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public CacheControl cacheControl()
/*     */   {
/* 102 */     CacheControl result = this.cacheControl;
/* 103 */     return result != null ? result : (this.cacheControl = CacheControl.parse(this.headers));
/*     */   }
/*     */   
/*     */   public boolean isHttps() {
/* 107 */     return this.url.isHttps();
/*     */   }
/*     */   
/*     */   public String toString() {
/* 111 */     return "Request{method=" + this.method + ", url=" + this.url + ", tag=" + (this.tag != this ? this.tag : null) + '}';
/*     */   }
/*     */   
/*     */ 
/*     */   public static class Builder
/*     */   {
/*     */     private HttpUrl url;
/*     */     
/*     */     private String method;
/*     */     
/*     */     private Headers.Builder headers;
/*     */     
/*     */     private RequestBody body;
/*     */     private Object tag;
/*     */     
/*     */     public Builder()
/*     */     {
/* 128 */       this.method = "GET";
/* 129 */       this.headers = new Headers.Builder();
/*     */     }
/*     */     
/*     */     private Builder(Request request) {
/* 133 */       this.url = request.url;
/* 134 */       this.method = request.method;
/* 135 */       this.body = request.body;
/* 136 */       this.tag = request.tag;
/* 137 */       this.headers = request.headers.newBuilder();
/*     */     }
/*     */     
/*     */     public Builder url(HttpUrl url) {
/* 141 */       if (url == null) throw new IllegalArgumentException("url == null");
/* 142 */       this.url = url;
/* 143 */       return this;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     public Builder url(String url)
/*     */     {
/* 153 */       if (url == null) { throw new IllegalArgumentException("url == null");
/*     */       }
/*     */       
/* 156 */       if (url.regionMatches(true, 0, "ws:", 0, 3)) {
/* 157 */         url = "http:" + url.substring(3);
/* 158 */       } else if (url.regionMatches(true, 0, "wss:", 0, 4)) {
/* 159 */         url = "https:" + url.substring(4);
/*     */       }
/*     */       
/* 162 */       HttpUrl parsed = HttpUrl.parse(url);
/* 163 */       if (parsed == null) throw new IllegalArgumentException("unexpected url: " + url);
/* 164 */       return url(parsed);
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     public Builder url(URL url)
/*     */     {
/* 174 */       if (url == null) throw new IllegalArgumentException("url == null");
/* 175 */       HttpUrl parsed = HttpUrl.get(url);
/* 176 */       if (parsed == null) throw new IllegalArgumentException("unexpected url: " + url);
/* 177 */       return url(parsed);
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */     public Builder header(String name, String value)
/*     */     {
/* 185 */       this.headers.set(name, value);
/* 186 */       return this;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     public Builder addHeader(String name, String value)
/*     */     {
/* 197 */       this.headers.add(name, value);
/* 198 */       return this;
/*     */     }
/*     */     
/*     */     public Builder removeHeader(String name) {
/* 202 */       this.headers.removeAll(name);
/* 203 */       return this;
/*     */     }
/*     */     
/*     */     public Builder headers(Headers headers)
/*     */     {
/* 208 */       this.headers = headers.newBuilder();
/* 209 */       return this;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     public Builder cacheControl(CacheControl cacheControl)
/*     */     {
/* 218 */       String value = cacheControl.toString();
/* 219 */       if (value.isEmpty()) return removeHeader("Cache-Control");
/* 220 */       return header("Cache-Control", value);
/*     */     }
/*     */     
/*     */     public Builder get() {
/* 224 */       return method("GET", null);
/*     */     }
/*     */     
/*     */     public Builder head() {
/* 228 */       return method("HEAD", null);
/*     */     }
/*     */     
/*     */     public Builder post(RequestBody body) {
/* 232 */       return method("POST", body);
/*     */     }
/*     */     
/*     */     public Builder delete(RequestBody body) {
/* 236 */       return method("DELETE", body);
/*     */     }
/*     */     
/*     */     public Builder delete() {
/* 240 */       return delete(RequestBody.create(null, new byte[0]));
/*     */     }
/*     */     
/*     */     public Builder put(RequestBody body) {
/* 244 */       return method("PUT", body);
/*     */     }
/*     */     
/*     */     public Builder patch(RequestBody body) {
/* 248 */       return method("PATCH", body);
/*     */     }
/*     */     
/*     */     public Builder method(String method, RequestBody body) {
/* 252 */       if ((method == null) || (method.length() == 0)) {
/* 253 */         throw new IllegalArgumentException("method == null || method.length() == 0");
/*     */       }
/* 255 */       if ((body != null) && (!HttpMethod.permitsRequestBody(method))) {
/* 256 */         throw new IllegalArgumentException("method " + method + " must not have a request body.");
/*     */       }
/* 258 */       if ((body == null) && (HttpMethod.requiresRequestBody(method))) {
/* 259 */         throw new IllegalArgumentException("method " + method + " must have a request body.");
/*     */       }
/* 261 */       this.method = method;
/* 262 */       this.body = body;
/* 263 */       return this;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     public Builder tag(Object tag)
/*     */     {
/* 272 */       this.tag = tag;
/* 273 */       return this;
/*     */     }
/*     */     
/*     */     public Request build() {
/* 277 */       if (this.url == null) throw new IllegalStateException("url == null");
/* 278 */       return new Request(this, null);
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\Request.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */