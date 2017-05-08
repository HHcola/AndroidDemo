/*     */ package com.squareup.okhttp.internal.framed;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Arrays;
/*     */ import java.util.Collections;
/*     */ import java.util.LinkedHashMap;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import okio.Buffer;
/*     */ import okio.BufferedSource;
/*     */ import okio.ByteString;
/*     */ import okio.Okio;
/*     */ import okio.Source;
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
/*     */ final class Hpack
/*     */ {
/*     */   private static final int PREFIX_4_BITS = 15;
/*     */   private static final int PREFIX_5_BITS = 31;
/*     */   private static final int PREFIX_6_BITS = 63;
/*     */   private static final int PREFIX_7_BITS = 127;
/*  46 */   private static final Header[] STATIC_HEADER_TABLE = { new Header(Header.TARGET_AUTHORITY, ""), new Header(Header.TARGET_METHOD, "GET"), new Header(Header.TARGET_METHOD, "POST"), new Header(Header.TARGET_PATH, "/"), new Header(Header.TARGET_PATH, "/index.html"), new Header(Header.TARGET_SCHEME, "http"), new Header(Header.TARGET_SCHEME, "https"), new Header(Header.RESPONSE_STATUS, "200"), new Header(Header.RESPONSE_STATUS, "204"), new Header(Header.RESPONSE_STATUS, "206"), new Header(Header.RESPONSE_STATUS, "304"), new Header(Header.RESPONSE_STATUS, "400"), new Header(Header.RESPONSE_STATUS, "404"), new Header(Header.RESPONSE_STATUS, "500"), new Header("accept-charset", ""), new Header("accept-encoding", "gzip, deflate"), new Header("accept-language", ""), new Header("accept-ranges", ""), new Header("accept", ""), new Header("access-control-allow-origin", ""), new Header("age", ""), new Header("allow", ""), new Header("authorization", ""), new Header("cache-control", ""), new Header("content-disposition", ""), new Header("content-encoding", ""), new Header("content-language", ""), new Header("content-length", ""), new Header("content-location", ""), new Header("content-range", ""), new Header("content-type", ""), new Header("cookie", ""), new Header("date", ""), new Header("etag", ""), new Header("expect", ""), new Header("expires", ""), new Header("from", ""), new Header("host", ""), new Header("if-match", ""), new Header("if-modified-since", ""), new Header("if-none-match", ""), new Header("if-range", ""), new Header("if-unmodified-since", ""), new Header("last-modified", ""), new Header("link", ""), new Header("location", ""), new Header("max-forwards", ""), new Header("proxy-authenticate", ""), new Header("proxy-authorization", ""), new Header("range", ""), new Header("referer", ""), new Header("refresh", ""), new Header("retry-after", ""), new Header("server", ""), new Header("set-cookie", ""), new Header("strict-transport-security", ""), new Header("transfer-encoding", ""), new Header("user-agent", ""), new Header("vary", ""), new Header("via", ""), new Header("www-authenticate", "") };
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
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   static final class Reader
/*     */   {
/* 116 */     private final List<Header> headerList = new ArrayList();
/*     */     
/*     */     private final BufferedSource source;
/*     */     
/*     */     private int headerTableSizeSetting;
/*     */     private int maxDynamicTableByteCount;
/* 122 */     Header[] dynamicTable = new Header[8];
/*     */     
/* 124 */     int nextHeaderIndex = this.dynamicTable.length - 1;
/* 125 */     int headerCount = 0;
/* 126 */     int dynamicTableByteCount = 0;
/*     */     
/*     */     Reader(int headerTableSizeSetting, Source source) {
/* 129 */       this.headerTableSizeSetting = headerTableSizeSetting;
/* 130 */       this.maxDynamicTableByteCount = headerTableSizeSetting;
/* 131 */       this.source = Okio.buffer(source);
/*     */     }
/*     */     
/*     */     int maxDynamicTableByteCount() {
/* 135 */       return this.maxDynamicTableByteCount;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     void headerTableSizeSetting(int headerTableSizeSetting)
/*     */     {
/* 146 */       this.headerTableSizeSetting = headerTableSizeSetting;
/* 147 */       this.maxDynamicTableByteCount = headerTableSizeSetting;
/* 148 */       adjustDynamicTableByteCount();
/*     */     }
/*     */     
/*     */     private void adjustDynamicTableByteCount() {
/* 152 */       if (this.maxDynamicTableByteCount < this.dynamicTableByteCount) {
/* 153 */         if (this.maxDynamicTableByteCount == 0) {
/* 154 */           clearDynamicTable();
/*     */         } else {
/* 156 */           evictToRecoverBytes(this.dynamicTableByteCount - this.maxDynamicTableByteCount);
/*     */         }
/*     */       }
/*     */     }
/*     */     
/*     */     private void clearDynamicTable() {
/* 162 */       this.headerList.clear();
/* 163 */       Arrays.fill(this.dynamicTable, null);
/* 164 */       this.nextHeaderIndex = (this.dynamicTable.length - 1);
/* 165 */       this.headerCount = 0;
/* 166 */       this.dynamicTableByteCount = 0;
/*     */     }
/*     */     
/*     */     private int evictToRecoverBytes(int bytesToRecover)
/*     */     {
/* 171 */       int entriesToEvict = 0;
/* 172 */       if (bytesToRecover > 0)
/*     */       {
/* 174 */         for (int j = this.dynamicTable.length - 1; (j >= this.nextHeaderIndex) && (bytesToRecover > 0); j--) {
/* 175 */           bytesToRecover -= this.dynamicTable[j].hpackSize;
/* 176 */           this.dynamicTableByteCount -= this.dynamicTable[j].hpackSize;
/* 177 */           this.headerCount -= 1;
/* 178 */           entriesToEvict++;
/*     */         }
/* 180 */         System.arraycopy(this.dynamicTable, this.nextHeaderIndex + 1, this.dynamicTable, this.nextHeaderIndex + 1 + entriesToEvict, this.headerCount);
/*     */         
/* 182 */         this.nextHeaderIndex += entriesToEvict;
/*     */       }
/* 184 */       return entriesToEvict;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */     void readHeaders()
/*     */       throws IOException
/*     */     {
/* 192 */       while (!this.source.exhausted()) {
/* 193 */         int b = this.source.readByte() & 0xFF;
/* 194 */         if (b == 128)
/* 195 */           throw new IOException("index == 0");
/* 196 */         if ((b & 0x80) == 128) {
/* 197 */           int index = readInt(b, 127);
/* 198 */           readIndexedHeader(index - 1);
/* 199 */         } else if (b == 64) {
/* 200 */           readLiteralHeaderWithIncrementalIndexingNewName();
/* 201 */         } else if ((b & 0x40) == 64) {
/* 202 */           int index = readInt(b, 63);
/* 203 */           readLiteralHeaderWithIncrementalIndexingIndexedName(index - 1);
/* 204 */         } else if ((b & 0x20) == 32) {
/* 205 */           this.maxDynamicTableByteCount = readInt(b, 31);
/* 206 */           if ((this.maxDynamicTableByteCount < 0) || (this.maxDynamicTableByteCount > this.headerTableSizeSetting))
/*     */           {
/* 208 */             throw new IOException("Invalid dynamic table size update " + this.maxDynamicTableByteCount);
/*     */           }
/* 210 */           adjustDynamicTableByteCount();
/* 211 */         } else if ((b == 16) || (b == 0)) {
/* 212 */           readLiteralHeaderWithoutIndexingNewName();
/*     */         } else {
/* 214 */           int index = readInt(b, 15);
/* 215 */           readLiteralHeaderWithoutIndexingIndexedName(index - 1);
/*     */         }
/*     */       }
/*     */     }
/*     */     
/*     */     public List<Header> getAndResetHeaderList() {
/* 221 */       List<Header> result = new ArrayList(this.headerList);
/* 222 */       this.headerList.clear();
/* 223 */       return result;
/*     */     }
/*     */     
/*     */     private void readIndexedHeader(int index) throws IOException {
/* 227 */       if (isStaticHeader(index)) {
/* 228 */         Header staticEntry = Hpack.STATIC_HEADER_TABLE[index];
/* 229 */         this.headerList.add(staticEntry);
/*     */       } else {
/* 231 */         int dynamicTableIndex = dynamicTableIndex(index - Hpack.STATIC_HEADER_TABLE.length);
/* 232 */         if ((dynamicTableIndex < 0) || (dynamicTableIndex > this.dynamicTable.length - 1)) {
/* 233 */           throw new IOException("Header index too large " + (index + 1));
/*     */         }
/* 235 */         this.headerList.add(this.dynamicTable[dynamicTableIndex]);
/*     */       }
/*     */     }
/*     */     
/*     */     private int dynamicTableIndex(int index)
/*     */     {
/* 241 */       return this.nextHeaderIndex + 1 + index;
/*     */     }
/*     */     
/*     */     private void readLiteralHeaderWithoutIndexingIndexedName(int index) throws IOException {
/* 245 */       ByteString name = getName(index);
/* 246 */       ByteString value = readByteString();
/* 247 */       this.headerList.add(new Header(name, value));
/*     */     }
/*     */     
/*     */     private void readLiteralHeaderWithoutIndexingNewName() throws IOException {
/* 251 */       ByteString name = Hpack.checkLowercase(readByteString());
/* 252 */       ByteString value = readByteString();
/* 253 */       this.headerList.add(new Header(name, value));
/*     */     }
/*     */     
/*     */     private void readLiteralHeaderWithIncrementalIndexingIndexedName(int nameIndex) throws IOException
/*     */     {
/* 258 */       ByteString name = getName(nameIndex);
/* 259 */       ByteString value = readByteString();
/* 260 */       insertIntoDynamicTable(-1, new Header(name, value));
/*     */     }
/*     */     
/*     */     private void readLiteralHeaderWithIncrementalIndexingNewName() throws IOException {
/* 264 */       ByteString name = Hpack.checkLowercase(readByteString());
/* 265 */       ByteString value = readByteString();
/* 266 */       insertIntoDynamicTable(-1, new Header(name, value));
/*     */     }
/*     */     
/*     */     private ByteString getName(int index) {
/* 270 */       if (isStaticHeader(index)) {
/* 271 */         return Hpack.STATIC_HEADER_TABLE[index].name;
/*     */       }
/* 273 */       return this.dynamicTable[dynamicTableIndex(index - Hpack.STATIC_HEADER_TABLE.length)].name;
/*     */     }
/*     */     
/*     */     private boolean isStaticHeader(int index)
/*     */     {
/* 278 */       return (index >= 0) && (index <= Hpack.STATIC_HEADER_TABLE.length - 1);
/*     */     }
/*     */     
/*     */     private void insertIntoDynamicTable(int index, Header entry)
/*     */     {
/* 283 */       this.headerList.add(entry);
/*     */       
/* 285 */       int delta = entry.hpackSize;
/* 286 */       if (index != -1) {
/* 287 */         delta -= this.dynamicTable[dynamicTableIndex(index)].hpackSize;
/*     */       }
/*     */       
/*     */ 
/* 291 */       if (delta > this.maxDynamicTableByteCount) {
/* 292 */         clearDynamicTable();
/* 293 */         return;
/*     */       }
/*     */       
/*     */ 
/* 297 */       int bytesToRecover = this.dynamicTableByteCount + delta - this.maxDynamicTableByteCount;
/* 298 */       int entriesEvicted = evictToRecoverBytes(bytesToRecover);
/*     */       
/* 300 */       if (index == -1) {
/* 301 */         if (this.headerCount + 1 > this.dynamicTable.length) {
/* 302 */           Header[] doubled = new Header[this.dynamicTable.length * 2];
/* 303 */           System.arraycopy(this.dynamicTable, 0, doubled, this.dynamicTable.length, this.dynamicTable.length);
/* 304 */           this.nextHeaderIndex = (this.dynamicTable.length - 1);
/* 305 */           this.dynamicTable = doubled;
/*     */         }
/* 307 */         index = this.nextHeaderIndex--;
/* 308 */         this.dynamicTable[index] = entry;
/* 309 */         this.headerCount += 1;
/*     */       } else {
/* 311 */         index += dynamicTableIndex(index) + entriesEvicted;
/* 312 */         this.dynamicTable[index] = entry;
/*     */       }
/* 314 */       this.dynamicTableByteCount += delta;
/*     */     }
/*     */     
/*     */     private int readByte() throws IOException {
/* 318 */       return this.source.readByte() & 0xFF;
/*     */     }
/*     */     
/*     */     int readInt(int firstByte, int prefixMask) throws IOException {
/* 322 */       int prefix = firstByte & prefixMask;
/* 323 */       if (prefix < prefixMask) {
/* 324 */         return prefix;
/*     */       }
/*     */       
/*     */ 
/* 328 */       int result = prefixMask;
/* 329 */       int shift = 0;
/*     */       for (;;) {
/* 331 */         int b = readByte();
/* 332 */         if ((b & 0x80) != 0) {
/* 333 */           result += ((b & 0x7F) << shift);
/* 334 */           shift += 7;
/*     */         } else {
/* 336 */           result += (b << shift);
/* 337 */           break;
/*     */         }
/*     */       }
/* 340 */       return result;
/*     */     }
/*     */     
/*     */     ByteString readByteString() throws IOException
/*     */     {
/* 345 */       int firstByte = readByte();
/* 346 */       boolean huffmanDecode = (firstByte & 0x80) == 128;
/* 347 */       int length = readInt(firstByte, 127);
/*     */       
/* 349 */       if (huffmanDecode) {
/* 350 */         return ByteString.of(Huffman.get().decode(this.source.readByteArray(length)));
/*     */       }
/* 352 */       return this.source.readByteString(length);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/* 357 */   private static final Map<ByteString, Integer> NAME_TO_FIRST_INDEX = nameToFirstIndex();
/*     */   
/*     */   private static Map<ByteString, Integer> nameToFirstIndex() {
/* 360 */     Map<ByteString, Integer> result = new LinkedHashMap(STATIC_HEADER_TABLE.length);
/* 361 */     for (int i = 0; i < STATIC_HEADER_TABLE.length; i++) {
/* 362 */       if (!result.containsKey(STATIC_HEADER_TABLE[i].name)) {
/* 363 */         result.put(STATIC_HEADER_TABLE[i].name, Integer.valueOf(i));
/*     */       }
/*     */     }
/* 366 */     return Collections.unmodifiableMap(result);
/*     */   }
/*     */   
/*     */   static final class Writer {
/*     */     private final Buffer out;
/*     */     
/*     */     Writer(Buffer out) {
/* 373 */       this.out = out;
/*     */     }
/*     */     
/*     */ 
/*     */     void writeHeaders(List<Header> headerBlock)
/*     */       throws IOException
/*     */     {
/* 380 */       int i = 0; for (int size = headerBlock.size(); i < size; i++) {
/* 381 */         ByteString name = ((Header)headerBlock.get(i)).name.toAsciiLowercase();
/* 382 */         Integer staticIndex = (Integer)Hpack.NAME_TO_FIRST_INDEX.get(name);
/* 383 */         if (staticIndex != null)
/*     */         {
/* 385 */           writeInt(staticIndex.intValue() + 1, 15, 0);
/* 386 */           writeByteString(((Header)headerBlock.get(i)).value);
/*     */         } else {
/* 388 */           this.out.writeByte(0);
/* 389 */           writeByteString(name);
/* 390 */           writeByteString(((Header)headerBlock.get(i)).value);
/*     */         }
/*     */       }
/*     */     }
/*     */     
/*     */     void writeInt(int value, int prefixMask, int bits)
/*     */       throws IOException
/*     */     {
/* 398 */       if (value < prefixMask) {
/* 399 */         this.out.writeByte(bits | value);
/* 400 */         return;
/*     */       }
/*     */       
/*     */ 
/* 404 */       this.out.writeByte(bits | prefixMask);
/* 405 */       value -= prefixMask;
/*     */       
/*     */ 
/* 408 */       while (value >= 128) {
/* 409 */         int b = value & 0x7F;
/* 410 */         this.out.writeByte(b | 0x80);
/* 411 */         value >>>= 7;
/*     */       }
/* 413 */       this.out.writeByte(value);
/*     */     }
/*     */     
/*     */     void writeByteString(ByteString data) throws IOException {
/* 417 */       writeInt(data.size(), 127, 0);
/* 418 */       this.out.write(data);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   private static ByteString checkLowercase(ByteString name)
/*     */     throws IOException
/*     */   {
/* 427 */     int i = 0; for (int length = name.size(); i < length; i++) {
/* 428 */       byte c = name.getByte(i);
/* 429 */       if ((c >= 65) && (c <= 90)) {
/* 430 */         throw new IOException("PROTOCOL_ERROR response malformed: mixed case name: " + name.utf8());
/*     */       }
/*     */     }
/* 433 */     return name;
/*     */   }
/*     */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\framed\Hpack.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */