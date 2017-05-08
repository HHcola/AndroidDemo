/*      */ package com.squareup.okhttp.internal;
/*      */ 
/*      */ import com.squareup.okhttp.internal.io.FileSystem;
/*      */ import java.io.Closeable;
/*      */ import java.io.EOFException;
/*      */ import java.io.File;
/*      */ import java.io.FileNotFoundException;
/*      */ import java.io.IOException;
/*      */ import java.util.ArrayList;
/*      */ import java.util.Arrays;
/*      */ import java.util.Collection;
/*      */ import java.util.Iterator;
/*      */ import java.util.LinkedHashMap;
/*      */ import java.util.NoSuchElementException;
/*      */ import java.util.concurrent.Executor;
/*      */ import java.util.concurrent.LinkedBlockingQueue;
/*      */ import java.util.concurrent.ThreadPoolExecutor;
/*      */ import java.util.concurrent.TimeUnit;
/*      */ import java.util.regex.Matcher;
/*      */ import java.util.regex.Pattern;
/*      */ import okio.Buffer;
/*      */ import okio.BufferedSink;
/*      */ import okio.BufferedSource;
/*      */ import okio.Okio;
/*      */ import okio.Sink;
/*      */ import okio.Source;
/*      */ import okio.Timeout;
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ public final class DiskLruCache
/*      */   implements Closeable
/*      */ {
/*      */   static final String JOURNAL_FILE = "journal";
/*      */   static final String JOURNAL_FILE_TEMP = "journal.tmp";
/*      */   static final String JOURNAL_FILE_BACKUP = "journal.bkp";
/*      */   static final String MAGIC = "libcore.io.DiskLruCache";
/*      */   static final String VERSION_1 = "1";
/*      */   static final long ANY_SEQUENCE_NUMBER = -1L;
/*   95 */   static final Pattern LEGAL_KEY_PATTERN = Pattern.compile("[a-z0-9_-]{1,120}");
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */   private static final String CLEAN = "CLEAN";
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */   private static final String DIRTY = "DIRTY";
/*      */   
/*      */ 
/*      */ 
/*      */   private static final String REMOVE = "REMOVE";
/*      */   
/*      */ 
/*      */ 
/*      */   private static final String READ = "READ";
/*      */   
/*      */ 
/*      */ 
/*      */   private final FileSystem fileSystem;
/*      */   
/*      */ 
/*      */ 
/*      */   private final File directory;
/*      */   
/*      */ 
/*      */ 
/*      */   private final File journalFile;
/*      */   
/*      */ 
/*      */ 
/*      */   private final File journalFileTmp;
/*      */   
/*      */ 
/*      */ 
/*      */   private final File journalFileBackup;
/*      */   
/*      */ 
/*      */ 
/*      */   private final int appVersion;
/*      */   
/*      */ 
/*      */ 
/*      */   private long maxSize;
/*      */   
/*      */ 
/*      */ 
/*      */   private final int valueCount;
/*      */   
/*      */ 
/*      */ 
/*  149 */   private long size = 0L;
/*      */   private BufferedSink journalWriter;
/*  151 */   private final LinkedHashMap<String, Entry> lruEntries = new LinkedHashMap(0, 0.75F, true);
/*      */   
/*      */ 
/*      */   private int redundantOpCount;
/*      */   
/*      */ 
/*      */   private boolean hasJournalErrors;
/*      */   
/*      */ 
/*      */   private boolean initialized;
/*      */   
/*      */   private boolean closed;
/*      */   
/*  164 */   private long nextSequenceNumber = 0L;
/*      */   
/*      */   private final Executor executor;
/*      */   
/*  168 */   private final Runnable cleanupRunnable = new Runnable() {
/*      */     public void run() {
/*  170 */       synchronized (DiskLruCache.this) {
/*  171 */         if ((!DiskLruCache.this.initialized | DiskLruCache.this.closed)) {
/*  172 */           return;
/*      */         }
/*      */         try {
/*  175 */           DiskLruCache.this.trimToSize();
/*  176 */           if (DiskLruCache.this.journalRebuildRequired()) {
/*  177 */             DiskLruCache.this.rebuildJournal();
/*  178 */             DiskLruCache.this.redundantOpCount = 0;
/*      */           }
/*      */         } catch (IOException e) {
/*  181 */           throw new RuntimeException(e);
/*      */         }
/*      */       }
/*      */     }
/*      */   };
/*      */   
/*      */   DiskLruCache(FileSystem fileSystem, File directory, int appVersion, int valueCount, long maxSize, Executor executor)
/*      */   {
/*  189 */     this.fileSystem = fileSystem;
/*  190 */     this.directory = directory;
/*  191 */     this.appVersion = appVersion;
/*  192 */     this.journalFile = new File(directory, "journal");
/*  193 */     this.journalFileTmp = new File(directory, "journal.tmp");
/*  194 */     this.journalFileBackup = new File(directory, "journal.bkp");
/*  195 */     this.valueCount = valueCount;
/*  196 */     this.maxSize = maxSize;
/*  197 */     this.executor = executor;
/*      */   }
/*      */   
/*      */   public synchronized void initialize() throws IOException {
/*  201 */     assert (Thread.holdsLock(this));
/*      */     
/*  203 */     if (this.initialized) {
/*  204 */       return;
/*      */     }
/*      */     
/*      */ 
/*  208 */     if (this.fileSystem.exists(this.journalFileBackup))
/*      */     {
/*  210 */       if (this.fileSystem.exists(this.journalFile)) {
/*  211 */         this.fileSystem.delete(this.journalFileBackup);
/*      */       } else {
/*  213 */         this.fileSystem.rename(this.journalFileBackup, this.journalFile);
/*      */       }
/*      */     }
/*      */     
/*      */ 
/*  218 */     if (this.fileSystem.exists(this.journalFile)) {
/*      */       try {
/*  220 */         readJournal();
/*  221 */         processJournal();
/*  222 */         this.initialized = true;
/*  223 */         return;
/*      */       } catch (IOException journalIsCorrupt) {
/*  225 */         Platform.get().logW("DiskLruCache " + this.directory + " is corrupt: " + journalIsCorrupt
/*  226 */           .getMessage() + ", removing");
/*  227 */         delete();
/*  228 */         this.closed = false;
/*      */       }
/*      */     }
/*      */     
/*  232 */     rebuildJournal();
/*      */     
/*  234 */     this.initialized = true;
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public static DiskLruCache create(FileSystem fileSystem, File directory, int appVersion, int valueCount, long maxSize)
/*      */   {
/*  247 */     if (maxSize <= 0L) {
/*  248 */       throw new IllegalArgumentException("maxSize <= 0");
/*      */     }
/*  250 */     if (valueCount <= 0) {
/*  251 */       throw new IllegalArgumentException("valueCount <= 0");
/*      */     }
/*      */     
/*      */ 
/*      */ 
/*  256 */     Executor executor = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue(), Util.threadFactory("OkHttp DiskLruCache", true));
/*      */     
/*  258 */     return new DiskLruCache(fileSystem, directory, appVersion, valueCount, maxSize, executor);
/*      */   }
/*      */   
/*      */   private void readJournal() throws IOException {
/*  262 */     BufferedSource source = Okio.buffer(this.fileSystem.source(this.journalFile));
/*      */     try {
/*  264 */       String magic = source.readUtf8LineStrict();
/*  265 */       String version = source.readUtf8LineStrict();
/*  266 */       String appVersionString = source.readUtf8LineStrict();
/*  267 */       String valueCountString = source.readUtf8LineStrict();
/*  268 */       String blank = source.readUtf8LineStrict();
/*  269 */       if ((!"libcore.io.DiskLruCache".equals(magic)) || 
/*  270 */         (!"1".equals(version)) || 
/*  271 */         (!Integer.toString(this.appVersion).equals(appVersionString)) || 
/*  272 */         (!Integer.toString(this.valueCount).equals(valueCountString)) || 
/*  273 */         (!"".equals(blank))) {
/*  274 */         throw new IOException("unexpected journal header: [" + magic + ", " + version + ", " + valueCountString + ", " + blank + "]");
/*      */       }
/*      */       
/*      */ 
/*  278 */       int lineCount = 0;
/*      */       try {
/*      */         for (;;) {
/*  281 */           readJournalLine(source.readUtf8LineStrict());
/*  282 */           lineCount++;
/*      */         }
/*      */         
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*  296 */         localObject = 
/*  297 */           finally;
/*      */       }
/*      */       catch (EOFException endOfJournal)
/*      */       {
/*  287 */         this.redundantOpCount = (lineCount - this.lruEntries.size());
/*      */         
/*      */ 
/*  290 */         if (!source.exhausted()) {
/*  291 */           rebuildJournal();
/*      */         } else
/*  293 */           this.journalWriter = newJournalWriter();
/*      */       }
/*      */       return;
/*  296 */     } finally { Util.closeQuietly(source);
/*      */     }
/*      */   }
/*      */   
/*      */   private BufferedSink newJournalWriter() throws FileNotFoundException {
/*  301 */     Sink fileSink = this.fileSystem.appendingSink(this.journalFile);
/*  302 */     Sink faultHidingSink = new FaultHidingSink(fileSink) {
/*      */       protected void onException(IOException e) {
/*  304 */         assert (Thread.holdsLock(DiskLruCache.this));
/*  305 */         DiskLruCache.this.hasJournalErrors = true;
/*      */       }
/*  307 */     };
/*  308 */     return Okio.buffer(faultHidingSink);
/*      */   }
/*      */   
/*      */   private void readJournalLine(String line) throws IOException {
/*  312 */     int firstSpace = line.indexOf(' ');
/*  313 */     if (firstSpace == -1) {
/*  314 */       throw new IOException("unexpected journal line: " + line);
/*      */     }
/*      */     
/*  317 */     int keyBegin = firstSpace + 1;
/*  318 */     int secondSpace = line.indexOf(' ', keyBegin);
/*      */     String key;
/*  320 */     if (secondSpace == -1) {
/*  321 */       String key = line.substring(keyBegin);
/*  322 */       if ((firstSpace == "REMOVE".length()) && (line.startsWith("REMOVE"))) {
/*  323 */         this.lruEntries.remove(key);
/*      */       }
/*      */     }
/*      */     else {
/*  327 */       key = line.substring(keyBegin, secondSpace);
/*      */     }
/*      */     
/*  330 */     Entry entry = (Entry)this.lruEntries.get(key);
/*  331 */     if (entry == null) {
/*  332 */       entry = new Entry(key, null);
/*  333 */       this.lruEntries.put(key, entry);
/*      */     }
/*      */     
/*  336 */     if ((secondSpace != -1) && (firstSpace == "CLEAN".length()) && (line.startsWith("CLEAN"))) {
/*  337 */       String[] parts = line.substring(secondSpace + 1).split(" ");
/*  338 */       entry.readable = true;
/*  339 */       entry.currentEditor = null;
/*  340 */       entry.setLengths(parts);
/*  341 */     } else if ((secondSpace == -1) && (firstSpace == "DIRTY".length()) && (line.startsWith("DIRTY"))) {
/*  342 */       entry.currentEditor = new Editor(entry, null);
/*  343 */     } else if ((secondSpace != -1) || (firstSpace != "READ".length()) || (!line.startsWith("READ")))
/*      */     {
/*      */ 
/*  346 */       throw new IOException("unexpected journal line: " + line);
/*      */     }
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */   private void processJournal()
/*      */     throws IOException
/*      */   {
/*  355 */     this.fileSystem.delete(this.journalFileTmp);
/*  356 */     for (Iterator<Entry> i = this.lruEntries.values().iterator(); i.hasNext();) {
/*  357 */       Entry entry = (Entry)i.next();
/*  358 */       if (entry.currentEditor == null) {
/*  359 */         for (int t = 0; t < this.valueCount; t++) {
/*  360 */           this.size += entry.lengths[t];
/*      */         }
/*      */       } else {
/*  363 */         entry.currentEditor = null;
/*  364 */         for (int t = 0; t < this.valueCount; t++) {
/*  365 */           this.fileSystem.delete(entry.cleanFiles[t]);
/*  366 */           this.fileSystem.delete(entry.dirtyFiles[t]);
/*      */         }
/*  368 */         i.remove();
/*      */       }
/*      */     }
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */   private synchronized void rebuildJournal()
/*      */     throws IOException
/*      */   {
/*  378 */     if (this.journalWriter != null) {
/*  379 */       this.journalWriter.close();
/*      */     }
/*      */     
/*  382 */     BufferedSink writer = Okio.buffer(this.fileSystem.sink(this.journalFileTmp));
/*      */     try {
/*  384 */       writer.writeUtf8("libcore.io.DiskLruCache").writeByte(10);
/*  385 */       writer.writeUtf8("1").writeByte(10);
/*  386 */       writer.writeDecimalLong(this.appVersion).writeByte(10);
/*  387 */       writer.writeDecimalLong(this.valueCount).writeByte(10);
/*  388 */       writer.writeByte(10);
/*      */       
/*  390 */       for (Entry entry : this.lruEntries.values()) {
/*  391 */         if (entry.currentEditor != null) {
/*  392 */           writer.writeUtf8("DIRTY").writeByte(32);
/*  393 */           writer.writeUtf8(entry.key);
/*  394 */           writer.writeByte(10);
/*      */         } else {
/*  396 */           writer.writeUtf8("CLEAN").writeByte(32);
/*  397 */           writer.writeUtf8(entry.key);
/*  398 */           entry.writeLengths(writer);
/*  399 */           writer.writeByte(10);
/*      */         }
/*      */       }
/*      */     } finally {
/*  403 */       writer.close();
/*      */     }
/*      */     
/*  406 */     if (this.fileSystem.exists(this.journalFile)) {
/*  407 */       this.fileSystem.rename(this.journalFile, this.journalFileBackup);
/*      */     }
/*  409 */     this.fileSystem.rename(this.journalFileTmp, this.journalFile);
/*  410 */     this.fileSystem.delete(this.journalFileBackup);
/*      */     
/*  412 */     this.journalWriter = newJournalWriter();
/*  413 */     this.hasJournalErrors = false;
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */   public synchronized Snapshot get(String key)
/*      */     throws IOException
/*      */   {
/*  422 */     initialize();
/*      */     
/*  424 */     checkNotClosed();
/*  425 */     validateKey(key);
/*  426 */     Entry entry = (Entry)this.lruEntries.get(key);
/*  427 */     if ((entry == null) || (!entry.readable)) { return null;
/*      */     }
/*  429 */     Snapshot snapshot = entry.snapshot();
/*  430 */     if (snapshot == null) { return null;
/*      */     }
/*  432 */     this.redundantOpCount += 1;
/*  433 */     this.journalWriter.writeUtf8("READ").writeByte(32).writeUtf8(key).writeByte(10);
/*  434 */     if (journalRebuildRequired()) {
/*  435 */       this.executor.execute(this.cleanupRunnable);
/*      */     }
/*      */     
/*  438 */     return snapshot;
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */   public Editor edit(String key)
/*      */     throws IOException
/*      */   {
/*  446 */     return edit(key, -1L);
/*      */   }
/*      */   
/*      */   private synchronized Editor edit(String key, long expectedSequenceNumber) throws IOException {
/*  450 */     initialize();
/*      */     
/*  452 */     checkNotClosed();
/*  453 */     validateKey(key);
/*  454 */     Entry entry = (Entry)this.lruEntries.get(key);
/*  455 */     if ((expectedSequenceNumber != -1L) && ((entry == null) || 
/*  456 */       (entry.sequenceNumber != expectedSequenceNumber))) {
/*  457 */       return null;
/*      */     }
/*  459 */     if ((entry != null) && (entry.currentEditor != null)) {
/*  460 */       return null;
/*      */     }
/*      */     
/*      */ 
/*  464 */     this.journalWriter.writeUtf8("DIRTY").writeByte(32).writeUtf8(key).writeByte(10);
/*  465 */     this.journalWriter.flush();
/*      */     
/*  467 */     if (this.hasJournalErrors) {
/*  468 */       return null;
/*      */     }
/*      */     
/*  471 */     if (entry == null) {
/*  472 */       entry = new Entry(key, null);
/*  473 */       this.lruEntries.put(key, entry);
/*      */     }
/*  475 */     Editor editor = new Editor(entry, null);
/*  476 */     entry.currentEditor = editor;
/*  477 */     return editor;
/*      */   }
/*      */   
/*      */   public File getDirectory()
/*      */   {
/*  482 */     return this.directory;
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */   public synchronized long getMaxSize()
/*      */   {
/*  490 */     return this.maxSize;
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */   public synchronized void setMaxSize(long maxSize)
/*      */   {
/*  498 */     this.maxSize = maxSize;
/*  499 */     if (this.initialized) {
/*  500 */       this.executor.execute(this.cleanupRunnable);
/*      */     }
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */   public synchronized long size()
/*      */     throws IOException
/*      */   {
/*  510 */     initialize();
/*  511 */     return this.size;
/*      */   }
/*      */   
/*      */   private synchronized void completeEdit(Editor editor, boolean success) throws IOException {
/*  515 */     Entry entry = editor.entry;
/*  516 */     if (entry.currentEditor != editor) {
/*  517 */       throw new IllegalStateException();
/*      */     }
/*      */     
/*      */ 
/*  521 */     if ((success) && (!entry.readable)) {
/*  522 */       for (int i = 0; i < this.valueCount; i++) {
/*  523 */         if (editor.written[i] == 0) {
/*  524 */           editor.abort();
/*  525 */           throw new IllegalStateException("Newly created entry didn't create value for index " + i);
/*      */         }
/*  527 */         if (!this.fileSystem.exists(entry.dirtyFiles[i])) {
/*  528 */           editor.abort();
/*  529 */           return;
/*      */         }
/*      */       }
/*      */     }
/*      */     
/*  534 */     for (int i = 0; i < this.valueCount; i++) {
/*  535 */       File dirty = entry.dirtyFiles[i];
/*  536 */       if (success) {
/*  537 */         if (this.fileSystem.exists(dirty)) {
/*  538 */           File clean = entry.cleanFiles[i];
/*  539 */           this.fileSystem.rename(dirty, clean);
/*  540 */           long oldLength = entry.lengths[i];
/*  541 */           long newLength = this.fileSystem.size(clean);
/*  542 */           entry.lengths[i] = newLength;
/*  543 */           this.size = (this.size - oldLength + newLength);
/*      */         }
/*      */       } else {
/*  546 */         this.fileSystem.delete(dirty);
/*      */       }
/*      */     }
/*      */     
/*  550 */     this.redundantOpCount += 1;
/*  551 */     entry.currentEditor = null;
/*  552 */     if ((entry.readable | success)) {
/*  553 */       entry.readable = true;
/*  554 */       this.journalWriter.writeUtf8("CLEAN").writeByte(32);
/*  555 */       this.journalWriter.writeUtf8(entry.key);
/*  556 */       entry.writeLengths(this.journalWriter);
/*  557 */       this.journalWriter.writeByte(10);
/*  558 */       if (success) {
/*  559 */         entry.sequenceNumber = (this.nextSequenceNumber++);
/*      */       }
/*      */     } else {
/*  562 */       this.lruEntries.remove(entry.key);
/*  563 */       this.journalWriter.writeUtf8("REMOVE").writeByte(32);
/*  564 */       this.journalWriter.writeUtf8(entry.key);
/*  565 */       this.journalWriter.writeByte(10);
/*      */     }
/*  567 */     this.journalWriter.flush();
/*      */     
/*  569 */     if ((this.size > this.maxSize) || (journalRebuildRequired())) {
/*  570 */       this.executor.execute(this.cleanupRunnable);
/*      */     }
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */   private boolean journalRebuildRequired()
/*      */   {
/*  579 */     int redundantOpCompactThreshold = 2000;
/*      */     
/*  581 */     return (this.redundantOpCount >= 2000) && (this.redundantOpCount >= this.lruEntries.size());
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public synchronized boolean remove(String key)
/*      */     throws IOException
/*      */   {
/*  592 */     initialize();
/*      */     
/*  594 */     checkNotClosed();
/*  595 */     validateKey(key);
/*  596 */     Entry entry = (Entry)this.lruEntries.get(key);
/*  597 */     if (entry == null) return false;
/*  598 */     return removeEntry(entry);
/*      */   }
/*      */   
/*      */   private boolean removeEntry(Entry entry) throws IOException {
/*  602 */     if (entry.currentEditor != null) {
/*  603 */       entry.currentEditor.hasErrors = true;
/*      */     }
/*      */     
/*  606 */     for (int i = 0; i < this.valueCount; i++) {
/*  607 */       this.fileSystem.delete(entry.cleanFiles[i]);
/*  608 */       this.size -= entry.lengths[i];
/*  609 */       entry.lengths[i] = 0L;
/*      */     }
/*      */     
/*  612 */     this.redundantOpCount += 1;
/*  613 */     this.journalWriter.writeUtf8("REMOVE").writeByte(32).writeUtf8(entry.key).writeByte(10);
/*  614 */     this.lruEntries.remove(entry.key);
/*      */     
/*  616 */     if (journalRebuildRequired()) {
/*  617 */       this.executor.execute(this.cleanupRunnable);
/*      */     }
/*      */     
/*  620 */     return true;
/*      */   }
/*      */   
/*      */   public synchronized boolean isClosed()
/*      */   {
/*  625 */     return this.closed;
/*      */   }
/*      */   
/*      */   private synchronized void checkNotClosed() {
/*  629 */     if (isClosed()) {
/*  630 */       throw new IllegalStateException("cache is closed");
/*      */     }
/*      */   }
/*      */   
/*      */   public synchronized void flush() throws IOException
/*      */   {
/*  636 */     if (!this.initialized) { return;
/*      */     }
/*  638 */     checkNotClosed();
/*  639 */     trimToSize();
/*  640 */     this.journalWriter.flush();
/*      */   }
/*      */   
/*      */   public synchronized void close() throws IOException
/*      */   {
/*  645 */     if ((!this.initialized) || (this.closed)) {
/*  646 */       this.closed = true;
/*  647 */       return;
/*      */     }
/*      */     
/*  650 */     for (Entry entry : (Entry[])this.lruEntries.values().toArray(new Entry[this.lruEntries.size()])) {
/*  651 */       if (entry.currentEditor != null) {
/*  652 */         entry.currentEditor.abort();
/*      */       }
/*      */     }
/*  655 */     trimToSize();
/*  656 */     this.journalWriter.close();
/*  657 */     this.journalWriter = null;
/*  658 */     this.closed = true;
/*      */   }
/*      */   
/*      */   private void trimToSize() throws IOException {
/*  662 */     while (this.size > this.maxSize) {
/*  663 */       Entry toEvict = (Entry)this.lruEntries.values().iterator().next();
/*  664 */       removeEntry(toEvict);
/*      */     }
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */   public void delete()
/*      */     throws IOException
/*      */   {
/*  674 */     close();
/*  675 */     this.fileSystem.deleteContents(this.directory);
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */   public synchronized void evictAll()
/*      */     throws IOException
/*      */   {
/*  683 */     initialize();
/*      */     
/*  685 */     for (Entry entry : (Entry[])this.lruEntries.values().toArray(new Entry[this.lruEntries.size()])) {
/*  686 */       removeEntry(entry);
/*      */     }
/*      */   }
/*      */   
/*      */   private void validateKey(String key) {
/*  691 */     Matcher matcher = LEGAL_KEY_PATTERN.matcher(key);
/*  692 */     if (!matcher.matches()) {
/*  693 */       throw new IllegalArgumentException("keys must match regex [a-z0-9_-]{1,120}: \"" + key + "\"");
/*      */     }
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public synchronized Iterator<Snapshot> snapshots()
/*      */     throws IOException
/*      */   {
/*  714 */     initialize();
/*  715 */     new Iterator()
/*      */     {
/*  717 */       final Iterator<DiskLruCache.Entry> delegate = new ArrayList(DiskLruCache.this.lruEntries.values()).iterator();
/*      */       
/*      */       DiskLruCache.Snapshot nextSnapshot;
/*      */       
/*      */       DiskLruCache.Snapshot removeSnapshot;
/*      */       
/*      */ 
/*      */       public boolean hasNext()
/*      */       {
/*  726 */         if (this.nextSnapshot != null) { return true;
/*      */         }
/*  728 */         synchronized (DiskLruCache.this)
/*      */         {
/*  730 */           if (DiskLruCache.this.closed) { return false;
/*      */           }
/*  732 */           while (this.delegate.hasNext()) {
/*  733 */             DiskLruCache.Entry entry = (DiskLruCache.Entry)this.delegate.next();
/*  734 */             DiskLruCache.Snapshot snapshot = entry.snapshot();
/*  735 */             if (snapshot != null) {
/*  736 */               this.nextSnapshot = snapshot;
/*  737 */               return true;
/*      */             }
/*      */           }
/*      */         }
/*  741 */         return false;
/*      */       }
/*      */       
/*      */       public DiskLruCache.Snapshot next() {
/*  745 */         if (!hasNext()) throw new NoSuchElementException();
/*  746 */         this.removeSnapshot = this.nextSnapshot;
/*  747 */         this.nextSnapshot = null;
/*  748 */         return this.removeSnapshot;
/*      */       }
/*      */       
/*      */       /* Error */
/*      */       public void remove()
/*      */       {
/*      */         // Byte code:
/*      */         //   0: aload_0
/*      */         //   1: getfield 18	com/squareup/okhttp/internal/DiskLruCache$3:removeSnapshot	Lcom/squareup/okhttp/internal/DiskLruCache$Snapshot;
/*      */         //   4: ifnonnull +13 -> 17
/*      */         //   7: new 19	java/lang/IllegalStateException
/*      */         //   10: dup
/*      */         //   11: ldc 20
/*      */         //   13: invokespecial 21	java/lang/IllegalStateException:<init>	(Ljava/lang/String;)V
/*      */         //   16: athrow
/*      */         //   17: aload_0
/*      */         //   18: getfield 1	com/squareup/okhttp/internal/DiskLruCache$3:this$0	Lcom/squareup/okhttp/internal/DiskLruCache;
/*      */         //   21: aload_0
/*      */         //   22: getfield 18	com/squareup/okhttp/internal/DiskLruCache$3:removeSnapshot	Lcom/squareup/okhttp/internal/DiskLruCache$Snapshot;
/*      */         //   25: invokestatic 22	com/squareup/okhttp/internal/DiskLruCache$Snapshot:access$2100	(Lcom/squareup/okhttp/internal/DiskLruCache$Snapshot;)Ljava/lang/String;
/*      */         //   28: invokevirtual 23	com/squareup/okhttp/internal/DiskLruCache:remove	(Ljava/lang/String;)Z
/*      */         //   31: pop
/*      */         //   32: aload_0
/*      */         //   33: aconst_null
/*      */         //   34: putfield 18	com/squareup/okhttp/internal/DiskLruCache$3:removeSnapshot	Lcom/squareup/okhttp/internal/DiskLruCache$Snapshot;
/*      */         //   37: goto +20 -> 57
/*      */         //   40: astore_1
/*      */         //   41: aload_0
/*      */         //   42: aconst_null
/*      */         //   43: putfield 18	com/squareup/okhttp/internal/DiskLruCache$3:removeSnapshot	Lcom/squareup/okhttp/internal/DiskLruCache$Snapshot;
/*      */         //   46: goto +11 -> 57
/*      */         //   49: astore_2
/*      */         //   50: aload_0
/*      */         //   51: aconst_null
/*      */         //   52: putfield 18	com/squareup/okhttp/internal/DiskLruCache$3:removeSnapshot	Lcom/squareup/okhttp/internal/DiskLruCache$Snapshot;
/*      */         //   55: aload_2
/*      */         //   56: athrow
/*      */         //   57: return
/*      */         // Line number table:
/*      */         //   Java source line #752	-> byte code offset #0
/*      */         //   Java source line #754	-> byte code offset #17
/*      */         //   Java source line #759	-> byte code offset #32
/*      */         //   Java source line #760	-> byte code offset #37
/*      */         //   Java source line #755	-> byte code offset #40
/*      */         //   Java source line #759	-> byte code offset #41
/*      */         //   Java source line #760	-> byte code offset #46
/*      */         //   Java source line #759	-> byte code offset #49
/*      */         //   Java source line #761	-> byte code offset #57
/*      */         // Local variable table:
/*      */         //   start	length	slot	name	signature
/*      */         //   0	58	0	this	3
/*      */         //   40	1	1	localIOException	IOException
/*      */         //   49	7	2	localObject	Object
/*      */         // Exception table:
/*      */         //   from	to	target	type
/*      */         //   17	32	40	java/io/IOException
/*      */         //   17	32	49	finally
/*      */       }
/*      */     };
/*      */   }
/*      */   
/*      */   public final class Snapshot
/*      */     implements Closeable
/*      */   {
/*      */     private final String key;
/*      */     private final long sequenceNumber;
/*      */     private final Source[] sources;
/*      */     private final long[] lengths;
/*      */     
/*      */     private Snapshot(String key, long sequenceNumber, Source[] sources, long[] lengths)
/*      */     {
/*  773 */       this.key = key;
/*  774 */       this.sequenceNumber = sequenceNumber;
/*  775 */       this.sources = sources;
/*  776 */       this.lengths = lengths;
/*      */     }
/*      */     
/*      */     public String key() {
/*  780 */       return this.key;
/*      */     }
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */     public DiskLruCache.Editor edit()
/*      */       throws IOException
/*      */     {
/*  789 */       return DiskLruCache.this.edit(this.key, this.sequenceNumber);
/*      */     }
/*      */     
/*      */     public Source getSource(int index)
/*      */     {
/*  794 */       return this.sources[index];
/*      */     }
/*      */     
/*      */     public long getLength(int index)
/*      */     {
/*  799 */       return this.lengths[index];
/*      */     }
/*      */     
/*      */     public void close() {
/*  803 */       for (Source in : this.sources) {
/*  804 */         Util.closeQuietly(in);
/*      */       }
/*      */     }
/*      */   }
/*      */   
/*  809 */   private static final Sink NULL_SINK = new Sink() {
/*      */     public void write(Buffer source, long byteCount) throws IOException {
/*  811 */       source.skip(byteCount);
/*      */     }
/*      */     
/*      */     public void flush() throws IOException
/*      */     {}
/*      */     
/*      */     public Timeout timeout() {
/*  818 */       return Timeout.NONE;
/*      */     }
/*      */     
/*      */     public void close() throws IOException
/*      */     {}
/*      */   };
/*      */   
/*      */   public final class Editor
/*      */   {
/*      */     private final DiskLruCache.Entry entry;
/*      */     private final boolean[] written;
/*      */     private boolean hasErrors;
/*      */     private boolean committed;
/*      */     
/*      */     private Editor(DiskLruCache.Entry entry) {
/*  833 */       this.entry = entry;
/*  834 */       this.written = (entry.readable ? null : new boolean[DiskLruCache.this.valueCount]);
/*      */     }
/*      */     
/*      */ 
/*      */ 
/*      */     public Source newSource(int index)
/*      */       throws IOException
/*      */     {
/*  842 */       synchronized (DiskLruCache.this) {
/*  843 */         if (this.entry.currentEditor != this) {
/*  844 */           throw new IllegalStateException();
/*      */         }
/*  846 */         if (!this.entry.readable) {
/*  847 */           return null;
/*      */         }
/*      */         try {
/*  850 */           return DiskLruCache.this.fileSystem.source(this.entry.cleanFiles[index]);
/*      */         } catch (FileNotFoundException e) {
/*  852 */           return null;
/*      */         }
/*      */       }
/*      */     }
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     public Sink newSink(int index)
/*      */       throws IOException
/*      */     {
/*  865 */       synchronized (DiskLruCache.this) {
/*  866 */         if (this.entry.currentEditor != this) {
/*  867 */           throw new IllegalStateException();
/*      */         }
/*  869 */         if (!this.entry.readable) {
/*  870 */           this.written[index] = true;
/*      */         }
/*  872 */         File dirtyFile = this.entry.dirtyFiles[index];
/*      */         try
/*      */         {
/*  875 */           sink = DiskLruCache.this.fileSystem.sink(dirtyFile);
/*      */         } catch (FileNotFoundException e) { Sink sink;
/*  877 */           return DiskLruCache.NULL_SINK; }
/*      */         Sink sink;
/*  879 */         new FaultHidingSink(sink) {
/*      */           protected void onException(IOException e) {
/*  881 */             synchronized (DiskLruCache.this) {
/*  882 */               DiskLruCache.Editor.this.hasErrors = true;
/*      */             }
/*      */           }
/*      */         };
/*      */       }
/*      */     }
/*      */     
/*      */ 
/*      */ 
/*      */     public void commit()
/*      */       throws IOException
/*      */     {
/*  894 */       synchronized (DiskLruCache.this) {
/*  895 */         if (this.hasErrors) {
/*  896 */           DiskLruCache.this.completeEdit(this, false);
/*  897 */           DiskLruCache.this.removeEntry(this.entry);
/*      */         } else {
/*  899 */           DiskLruCache.this.completeEdit(this, true);
/*      */         }
/*  901 */         this.committed = true;
/*      */       }
/*      */     }
/*      */     
/*      */ 
/*      */ 
/*      */     public void abort()
/*      */       throws IOException
/*      */     {
/*  910 */       synchronized (DiskLruCache.this) {
/*  911 */         DiskLruCache.this.completeEdit(this, false);
/*      */       }
/*      */     }
/*      */     
/*      */     public void abortUnlessCommitted() {
/*  916 */       synchronized (DiskLruCache.this) {
/*  917 */         if (!this.committed) {
/*      */           try {
/*  919 */             DiskLruCache.this.completeEdit(this, false);
/*      */           }
/*      */           catch (IOException localIOException) {}
/*      */         }
/*      */       }
/*      */     }
/*      */   }
/*      */   
/*      */ 
/*      */   private final class Entry
/*      */   {
/*      */     private final String key;
/*      */     
/*      */     private final long[] lengths;
/*      */     
/*      */     private final File[] cleanFiles;
/*      */     
/*      */     private final File[] dirtyFiles;
/*      */     
/*      */     private boolean readable;
/*      */     
/*      */     private DiskLruCache.Editor currentEditor;
/*      */     private long sequenceNumber;
/*      */     
/*      */     private Entry(String key)
/*      */     {
/*  945 */       this.key = key;
/*      */       
/*  947 */       this.lengths = new long[DiskLruCache.this.valueCount];
/*  948 */       this.cleanFiles = new File[DiskLruCache.this.valueCount];
/*  949 */       this.dirtyFiles = new File[DiskLruCache.this.valueCount];
/*      */       
/*      */ 
/*  952 */       StringBuilder fileBuilder = new StringBuilder(key).append('.');
/*  953 */       int truncateTo = fileBuilder.length();
/*  954 */       for (int i = 0; i < DiskLruCache.this.valueCount; i++) {
/*  955 */         fileBuilder.append(i);
/*  956 */         this.cleanFiles[i] = new File(DiskLruCache.this.directory, fileBuilder.toString());
/*  957 */         fileBuilder.append(".tmp");
/*  958 */         this.dirtyFiles[i] = new File(DiskLruCache.this.directory, fileBuilder.toString());
/*  959 */         fileBuilder.setLength(truncateTo);
/*      */       }
/*      */     }
/*      */     
/*      */     private void setLengths(String[] strings) throws IOException
/*      */     {
/*  965 */       if (strings.length != DiskLruCache.this.valueCount) {
/*  966 */         throw invalidLengths(strings);
/*      */       }
/*      */       try
/*      */       {
/*  970 */         for (int i = 0; i < strings.length; i++) {
/*  971 */           this.lengths[i] = Long.parseLong(strings[i]);
/*      */         }
/*      */       } catch (NumberFormatException e) {
/*  974 */         throw invalidLengths(strings);
/*      */       }
/*      */     }
/*      */     
/*      */     void writeLengths(BufferedSink writer) throws IOException
/*      */     {
/*  980 */       for (long length : this.lengths) {
/*  981 */         writer.writeByte(32).writeDecimalLong(length);
/*      */       }
/*      */     }
/*      */     
/*      */     private IOException invalidLengths(String[] strings) throws IOException {
/*  986 */       throw new IOException("unexpected journal line: " + Arrays.toString(strings));
/*      */     }
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */     DiskLruCache.Snapshot snapshot()
/*      */     {
/*  995 */       if (!Thread.holdsLock(DiskLruCache.this)) { throw new AssertionError();
/*      */       }
/*  997 */       Source[] sources = new Source[DiskLruCache.this.valueCount];
/*  998 */       long[] lengths = (long[])this.lengths.clone();
/*      */       try {
/* 1000 */         for (int i = 0; i < DiskLruCache.this.valueCount; i++) {
/* 1001 */           sources[i] = DiskLruCache.this.fileSystem.source(this.cleanFiles[i]);
/*      */         }
/* 1003 */         return new DiskLruCache.Snapshot(DiskLruCache.this, this.key, this.sequenceNumber, sources, lengths, null);
/*      */       }
/*      */       catch (FileNotFoundException e) {
/* 1006 */         for (int i = 0; i < DiskLruCache.this.valueCount; i++) {
/* 1007 */           if (sources[i] == null) break;
/* 1008 */           Util.closeQuietly(sources[i]);
/*      */         }
/*      */       }
/*      */       
/*      */ 
/* 1013 */       return null;
/*      */     }
/*      */   }
/*      */ }


/* Location:              C:\Users\hewei05\Desktop\okhttp-2.7.5.jar!\com\squareup\okhttp\internal\DiskLruCache.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */