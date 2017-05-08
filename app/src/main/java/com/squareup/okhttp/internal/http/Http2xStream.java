/*     */ package com.squareup.okhttp.internal.http;
/*     */ 
/*     */ import com.squareup.okhttp.Headers;
/*     */ import com.squareup.okhttp.Headers.Builder;
/*     */ import com.squareup.okhttp.HttpUrl;
/*     */ import com.squareup.okhttp.OkHttpClient;
/*     */ import com.squareup.okhttp.Protocol;
/*     */ import com.squareup.okhttp.Request;
/*     */ import com.squareup.okhttp.Response;
/*     */ import com.squareup.okhttp.Response.Builder;
/*     */ import com.squareup.okhttp.ResponseBody;
/*     */ import com.squareup.okhttp.internal.Util;
/*     */ import com.squareup.okhttp.internal.framed.ErrorCode;
/*     */ import com.squareup.okhttp.internal.framed.FramedConnection;
/*     */ import com.squareup.okhttp.internal.framed.FramedStream;
/*     */ import com.squareup.okhttp.internal.framed.Header;
/*     */ import java.io.IOException;
/*     */ import java.net.ProtocolException;
/*     */ import java.util.ArrayList;
/*     */ import java.util.LinkedHashSet;
/*     */ import java.util.List;
/*     */ import java.util.Locale;
/*     */ import java.util.Set;
/*     */ import java.util.concurrent.TimeUnit;
/*     */ import okio.ByteString;
/*     */ import okio.ForwardingSource;
/*     */ import okio.Okio;
/*     */ import okio.Sink;
/*     */ import okio.Source;
/*     */ import okio.Timeout;
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
/*     */ public final class Http2xStream
/*     */   implements HttpStream
/*     */ {
/*  53 */   private static final ByteString CONNECTION = ByteString.encodeUtf8("connection");
/*  54 */   private static final ByteString HOST = ByteString.encodeUtf8("host");
/*  55 */   private static final ByteString KEEP_ALIVE = ByteString.encodeUtf8("keep-alive");
/*  56 */   private static final ByteString PROXY_CONNECTION = ByteString.encodeUtf8("proxy-connection");
/*  57 */   private static final ByteString TRANSFER_ENCODING = ByteString.encodeUtf8("transfer-encoding");
/*  58 */   private static final ByteString TE = ByteString.encodeUtf8("te");
/*  59 */   private static final ByteString ENCODING = ByteString.encodeUtf8("encoding");
/*  60 */   private static final ByteString UPGRADE = ByteString.encodeUtf8("upgrade");
/*     */   
/*     */ 
/*  63 */   private static final List<ByteString> SPDY_3_SKIPPED_REQUEST_HEADERS = Util.immutableList(new ByteString[] { CONNECTION, HOST, KEEP_ALIVE, PROXY_CONNECTION, TRANSFER_ENCODING, Header.TARGET_METHOD, Header.TARGET_PATH, Header.TARGET_SCHEME, Header.TARGET_AUTHORITY, Header.TARGET_HOST, Header.VERSION });
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
/*  75 */   private static final List<ByteString> SPDY_3_SKIPPED_RESPONSE_HEADERS = Util.immutableList(new ByteString[] { CONNECTION, HOST, KEEP_ALIVE, PROXY_CONNECTION, TRANSFER_ENCODING });
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*  83 */   private static final List<ByteString> HTTP_2_SKIPPED_REQUEST_HEADERS = Util.immutableList(new ByteString[] { CONNECTION, HOST, KEEP_ALIVE, PROXY_CONNECTION, TE, TRANSFER_ENCODING, ENCODING, UPGRADE, Header.TARGET_METHOD, Header.TARGET_PATH, Header.TARGET_SCHEME, Header.TARGET_AUTHORITY, Header.TARGET_HOST, Header.VERSION });
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
/*  98 */   private static final List<ByteString> HTTP_2_SKIPPED_RESPONSE_HEADERS = Util.immutableList(new ByteString[] { CONNECTION, HOST, KEEP_ALIVE, PROXY_CONNECTION, TE, TRANSFER_ENCODING, ENCODING, UPGRADE });
/*     */   
/*     */ 
/*     */   private final StreamAllocation streamAllocation;
/*     */   
/*     */ 
/*     */   private final FramedConnection framedConnection;
/*     */   
/*     */ 
/*     */   private HttpEngine httpEngine;
/*     */   
/*     */   private FramedStream stream;
/*     */   
/*     */ 
/*     */   public Http2xStream(StreamAllocation streamAllocation, FramedConnection framedConnection)
/*     */   {
/* 114 */     this.streamAllocation = streamAllocation;
/* 115 */     this.framedConnection = framedConnection;
/*     */   }
/*     */   
/*     */   public void setHttpEngine(HttpEngine httpEngine) {
/* 119 */     this.httpEngine = httpEngine;
/*     */   }
/*     */   
/*     */   public Sink createRequestBody(Request request, long contentLength) throws IOException {
/* 123 */     return this.stream.getSink();
/*     */   }
/*     */   
/*     */   public void writeRequestHeaders(Request request) throws IOException {
/* 127 */     if (this.stream != null) { return;
/*     */     }
/* 129 */     this.httpEngine.writingRequestHeaders();
/* 130 */     boolean permitsRequestBody = this.httpEngine.permitsRequestBody(request);
/*     */     
/*     */ 
/* 133 */     List<Header> requestHeaders = this.framedConnection.getProtocol() == Protocol.HTTP_2 ? http2HeadersList(request) : spdy3HeadersList(request);
/* 134 */     boolean hasResponseBody = true;
/* 135 */     this.stream = this.framedConnection.newStream(requestHeaders, permitsRequestBody, hasResponseBody);
/* 136 */     this.stream.readTimeout().timeout(this.httpEngine.client.getReadTimeout(), TimeUnit.MILLISECONDS);
/* 137 */     this.stream.writeTimeout().timeout(this.httpEngine.client.getWriteTimeout(), TimeUnit.MILLISECONDS);
/*     */   }
/*     */   
/*     */   public void writeRequestBody(RetryableSink requestBody) throws IOException {
/* 141 */     requestBody.writeToSocket(this.stream.getSink());
/*     */   }
/*     */   
/*     */   public void finishRequest() throws IOException {
/* 145 */     this.stream.getSink().close();
/*     */   }
/*     */   
/*     */   public Response.Builder readResponseHeaders()
/*     */     throws IOException
/*     */   {
/* 151 */     return this.framedConnection.getProtocol() == Protocol.HTTP_2 ? readHttp2HeadersList(this.stream.getResponseHeaders()) : readSpdy3HeadersList(this.stream.getResponseHeaders());
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static List<Header> spdy3HeadersList(Request request)
/*     */   {
/* 160 */     Headers headers = request.headers();
/* 161 */     List<Header> result = new ArrayList(headers.size() + 5);
/* 162 */     result.add(new Header(Header.TARGET_METHOD, request.method()));
/* 163 */     result.add(new Header(Header.TARGET_PATH, RequestLine.requestPath(request.httpUrl())));
/* 164 */     result.add(new Header(Header.VERSION, "HTTP/1.1"));
/* 165 */     result.add(new Header(Header.TARGET_HOST, Util.hostHeader(request.httpUrl())));
/* 166 */     result.add(new Header(Header.TARGET_SCHEME, request.httpUrl().scheme()));
/*     */     
/* 168 */     Set<ByteString> names = new LinkedHashSet();
/* 169 */     int i = 0; for (int size = headers.size(); i < size; i++)
/*     */     {
/* 171 */       ByteString name = ByteString.encodeUtf8(headers.name(i).toLowerCase(Locale.US));
/*     */       
/*     */ 
/* 174 */       if (!SPDY_3_SKIPPED_REQUEST_HEADERS.contains(name))
/*     */       {
/*     */ 
/* 177 */         String value = headers.value(i);
/* 178 */         if (names.add(name)) {
/* 179 */           result.add(new Header(name, value));
/*     */ 
/*     */         }
/*     */         else
/*     */         {
/* 184 */           for (int j = 0; j < result.size(); j++)
/* 185 */             if (((Header)result.get(j)).name.equals(name)) {
/* 186 */               String concatenated = joinOnNull(((Header)result.get(j)).value.utf8(), value);
/* 187 */               result.set(j, new Header(name, concatenated));
/* 188 */               break;
/*     */             } }
/*     */       }
/*     */     }
/* 192 */     return result;
/*     */   }
/*     */   
/*     */   private static String joinOnNull(String first, String second) {
/* 196 */     return first + '\000' + second;
/*     */   }
/*     */   
/*     */   public static List<Header> http2HeadersList(Request request) {
/* 200 */     Headers headers = request.headers();
/* 201 */     List<Header> result = new ArrayList(headers.size() + 4);
/* 202 */     result.add(new Header(Header.TARGET_METHOD, request.method()));
/* 203 */     result.add(new Header(Header.TARGET_PATH, RequestLine.requestPath(request.httpUrl())));
/* 204 */     result.add(new Header(Header.TARGET_AUTHORITY, Util.hostHeader(request.httpUrl())));
/* 205 */     result.add(new Header(Header.TARGET_SCHEME, request.httpUrl().scheme()));
/*     */     
/* 207 */     int i = 0; for (int size = headers.size(); i < size; i++)
/*     */     {
/* 209 */       ByteString name = ByteString.encodeUtf8(headers.name(i).toLowerCase(Locale.US));
/* 210 */       if (!HTTP_2_SKIPPED_REQUEST_HEADERS.contains(name)) {
/* 211 */         result.add(new Header(name, headers.value(i)));
/*     */       }
/*     */     }
/* 214 */     return result;
/*     */   }
/*     */   
/*     */   public static Response.Builder readSpdy3HeadersList(List<Header> headerBlock) throws IOException
/*     */   {
/* 219 */     String status = null;
/* 220 */     String version = "HTTP/1.1";
/* 221 */     Headers.Builder headersBuilder = new Headers.Builder();
/* 222 */     int i = 0; ByteString name; String values; int start; for (int size = headerBlock.size(); i < size; i++) {
/* 223 */       name = ((Header)headerBlock.get(i)).name;
/*     */       
/* 225 */       values = ((Header)headerBlock.get(i)).value.utf8();
/* 226 */       for (start = 0; start < values.length();) {
/* 227 */         int end = values.indexOf(0, start);
/* 228 */         if (end == -1) {
/* 229 */           end = values.length();
/*     */         }
/* 231 */         String value = values.substring(start, end);
/* 232 */         if (name.equals(Header.RESPONSE_STATUS)) {
/* 233 */           status = value;
/* 234 */         } else if (name.equals(Header.VERSION)) {
/* 235 */           version = value;
/* 236 */         } else if (!SPDY_3_SKIPPED_RESPONSE_HEADERS.contains(name)) {
/* 237 */           headersBuilder.add(name.utf8(), value);
/*     */         }
/* 239 */         start = end + 1;
/*     */       }
/*     */     }
/* 242 */     if (status == null) { throw new ProtocolException("Expected ':status' header not present");
/*     */     }
/* 244 */     StatusLine statusLine = StatusLine.parse(version + " " + status);
/*     */     
/*     */ 
/*     */ 
/*     */ 
/* 249 */     return new Response.Builder().protocol(Protocol.SPDY_3).code(statusLine.code).message(statusLine.message).headers(headersBuilder.build());
/*     */   }
/*     */   
/*     */   public static Response.Builder readHttp2HeadersList(List<Header> headerBlock) throws IOException
/*     */   {
/* 254 */     String status = null;
/*     */     
/* 256 */     Headers.Builder headersBuilder = new Headers.Builder();
/* 257 */     int i = 0; for (int size = headerBlock.size(); i < size; i++) {
/* 258 */       ByteString name = ((Header)headerBlock.get(i)).name;
/*     */       
/* 260 */       String value = ((Header)headerBlock.get(i)).value.utf8();
/* 261 */       if (name.equals(Header.RESPONSE_STATUS)) {
/* 262 */         status = value;
/* 263 */       } else if (!HTTP_2_SKIPPED_RESPONSE_HEADERS.contains(name)) {
/* 264 */         headersBuilder.add(name.utf8(), value);
/*     */       }
/*     */     }
/* 267 */     if (status == null) { throw new ProtocolException("Expected ':status' header not present");
/*     */     }
/* 269 */     StatusLine statusLine = StatusLine.parse("HTTP/1.1 " + status);
/*     */     
/*     */ 
/*     */ 
/*     */ 
/* 274 */     return new Response.Builder().protocol(Protocol.HTTP_2).code(statusLine.code).message(statusLine.message).headers(headersBuilder.build());
/*     */   }
/*     */   
/*     */   public ResponseBody openResponseBody(Response response) throws IOException {
/* 278 */     Source source = new StreamFinishingSource(this.stream.getSource());
/* 279 */     return new RealResponseBody(response.headers(), Okio.buffer(source));
/*     */   }
/*     */   
/*     */   public void cancel() {
/* 283 */     if (this.stream != null) this.stream.closeLater(ErrorCode.CANCEL);
/*     */   }
/*     */   
/*     */   class StreamFinishingSource extends ForwardingSource {
/*     */     public StreamFinishingSource(Source delegate) {
/* 288 */       super();
/*     */     }
/*     */     
/*     */     public void close() throws IOException {
/* 292 */       Http2xStream.this.streamAllocation.streamFinished(Http2xStream.this);
/* 293 */       super.close();
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\http\Http2xStream.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */