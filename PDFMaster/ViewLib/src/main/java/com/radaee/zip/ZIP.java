package com.radaee.zip;

import com.radaee.comm.RDStream;

public class ZIP {
    private static native long openStream(RDStream str_obj);
    private static native long open(String path);
    private static native void close(long zip);
    private static native int getFileCount(long zip);
    private static native long getFileInfo(long zip, int idx);
    private static native String getFileName(long zip, int idx);
    private static native byte[] getFileDataToMem(long zip, int idx, String pswd);
    private static native boolean getFileDataToFile(long zip, int idx, String pswd, String path);
    private long hand;
    public ZIP()
    {
        hand = 0;
    }
    public boolean Open(String path)
    {
        hand = open(path);
        return (hand != 0);
    }
    public boolean Open(RDStream stream)
    {
        hand = openStream(stream);
        return (hand != 0);
    }
    public void Close()
    {
        close(hand);
    }

    /**
     * get count of entries.
     * @return count of entries.
     */
    public int GetFileCount()
    {
        return getFileCount(hand);
    }

    /**
     * is encrypted?
     * @param idx 0 based index, range [0, GetFileCount() - 1]
     * @return true or false.
     */
    public boolean IsEncrypted(int idx)
    {
        long info = getFileInfo(hand, idx);
        return ((info >> 48) & 1) != 0;
    }
    /**
     * is the entry directory?
     * @param idx 0 based index, range [0, GetFileCount() - 1]
     * @return true or false.
     */
    public boolean IsDir(int idx)
    {
        long info = getFileInfo(hand, idx);
        return ((info >> 49) & 1) != 0;
    }

    /**
     * get size of file(decoded)
     * @param idx 0 based index, range [0, GetFileCount() - 1]
     * @return decoded file size.
     */
    public long GetFileSize(int idx)
    {
        long info = getFileInfo(hand, idx);
        return ((info >> 49) & 0xffffffffffffL);
    }

    /**
     * get name of file, example: "dir/fname.png"
     * @param idx 0 based index, range [0, GetFileCount() - 1]
     * @return file name.
     */
    public String GetFileName(int idx)
    {
        return getFileName(hand, idx);
    }

    /**
     * get binary data to byte array.
     * @param idx 0 based index, range [0, GetFileCount() - 1]
     * @param pswd password, if encrypted.
     * @return byte array or null, return null mean invalid password or it is directory.
     */
    public byte[] GetFileData(int idx, String pswd)
    {
        return getFileDataToMem(hand, idx, pswd);
    }

    /**
     * save binary data to file.
     * @param idx 0 based index, range [0, GetFileCount() - 1]
     * @param pswd password, if encrypted.
     * @param path file name will be created.
     * @return true or false.
     */
    public boolean SaveFile(int idx, String pswd, String path)
    {
        return getFileDataToFile(hand, idx, pswd, path);
    }
}
