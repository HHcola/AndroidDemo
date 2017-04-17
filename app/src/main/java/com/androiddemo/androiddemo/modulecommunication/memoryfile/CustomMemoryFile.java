package com.androiddemo.androiddemo.modulecommunication.memoryfile;

import android.os.MemoryFile;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by hewei05 on 17/4/14.
 */

public class CustomMemoryFile {

    public static FileDescriptor fd = null;
    private static String fileName = "memfile";
    public static int mLength = 0;
    private static MemoryFile mf = null;

    /**
     * 写入共享内存
     * @param contentBytes
     */
    public static void onClientWriteData(byte[] contentBytes) {
        try {
            mLength = contentBytes.length;
            if (mf == null) {
                mf = new MemoryFile(fileName, 50);
            }
            mf.writeBytes(contentBytes, 0, 0, contentBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 通过共享内存文件描述符 读取内容
     * 返回读取的大小
     * @param contentBytes
     * @return
     */
    public static int onServiceReadData(byte[] contentBytes) {
        InputStream is = mf.getInputStream();
        try {

            is.read(contentBytes, 0, mLength);
        } catch (IOException ex) {
            // this is what should happen
        } finally {
            try {
                is.close();
                mLength = 0;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }
}
