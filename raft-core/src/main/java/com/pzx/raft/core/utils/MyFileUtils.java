package com.pzx.raft.core.utils;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

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

    /**
     * 读取目录下的所有文件
     *
     * @param dirPath
     * @return Map<String, byte[]> key为文件相对路径，byte[]文件文件数据
     * @throws IOException
     */
    public static Map<String, byte[]> readAllFiles(String dirPath) throws IOException{
        Map<String, byte[]> map = new HashMap<>();
        File file = new File(dirPath);
        if(!file.exists())
            return null;
        dfsDir(file, new ArrayList<>(), map);
        return map;
    }

    private static void dfsDir(File file, List<String> tmpList, Map<String, byte[]> map) throws IOException{
        if(file.isFile()){
            StringJoiner stringJoiner = new StringJoiner(File.separator);
            for(String s : tmpList)
                stringJoiner.add(s);
            map.put(stringJoiner.toString(), Files.readAllBytes(file.toPath()));
            return;
        }

        for(File subFile : file.listFiles()){
            tmpList.add(subFile.getName());
            dfsDir(subFile, tmpList, map);
            tmpList.remove(tmpList.size() - 1);
        }
    }




}
