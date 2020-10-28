package com.pzx.raft.utils;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class MyFileUtils {



    public static void writeObjectToFile(String filePath, Object object) throws IOException{
        try (OutputStream outputStream = new FileOutputStream(filePath)){
            outputStream.write(ProtobufSerializerUtils.serialize(object));
        }
    }

    public static <T> T readObjectFromFile(String filePath, Class<T> clazz) throws IOException{
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        Object object = ProtobufSerializerUtils.deserialize(bytes, clazz);
        return clazz.cast(object);
    }

    public static void mkDirIfNotExist(String dirPath){
        File dirFile = new File(dirPath);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
    }

    public static void deleteFileIfExist(String filePath) throws IOException{
        File file = new File(filePath);
        if (file.exists())
            FileUtils.forceDelete(file);

    }

    public static void deleteDirIfExist(String dirPath) throws IOException{
        File file = new File(dirPath);
        if (file.exists())
            FileUtils.deleteDirectory(file);
    }

    public static void moveDirectory(String dirPath, String dir1Path) throws IOException{
        FileUtils.moveDirectory(new File(dirPath), new File(dir1Path));
    }


    public static void createFileIfNotExist(String filePath)throws IOException {
        File file = new File(filePath);
        if (!file.exists())
            file.createNewFile();
    }

    public static String readFileToString(String filePath) throws IOException{
        File file = new File(filePath);
        return FileUtils.readFileToString(file, "utf-8");
    }




}
