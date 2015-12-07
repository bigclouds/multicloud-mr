package eu.leads.processor.common.utils;

/**
 * Created by tr on 19/11/2014.
 */

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.MapFile;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.hadoop.io.SequenceFile.Writer;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.util.ReflectionUtils;

import java.io.IOException;

//import tuc.softnet.log.TucLogger;

public class HDFSUtils
{
    final private static String empty = "";
    static Configuration conf = new Configuration();

    public static void addConfiguration(String key, String value)
    {
        conf.set(key, value);
    }

    public static void deletePath(String string, boolean recursive)
    {
        Path path = new Path(string);

        try
        {
            FileSystem fs = path.getFileSystem(conf);
            if (fs.exists(path))
                if (recursive)
                    fs.delete(path, true);
                else
                    fs.delete(path, false);
        } catch (IOException ioe)
        {
            System.out.println("IOEXCEPTION WHEN DELETING");
            ioe.printStackTrace();
        }
    }

    public static void deletePath(String string)
    {
        Path path = new Path(string);

        try
        {
            FileSystem fs = path.getFileSystem(conf);
            if (fs.exists(path))
                fs.delete(path, true);
        } catch (IOException ioe)
        {
            System.out.println("IOEXCEPTION WHEN DELETING");
            ioe.printStackTrace();
        }
    }

    public static void copyFiles(String srcDir, String destDir, String filter)
    {
        Path src = new Path(srcDir);
        Path dest = new Path(destDir);

        try
        {
            FileSystem fs = src.getFileSystem(conf);
            if (!fs.exists(src))
                return;
            for (FileStatus stat : fs.listStatus(src))
            {
                if (!filter.equals(empty))
                {
                    if (!stat.getPath().getName().matches(filter))
                    {
                        // System.out.printf("file %s filtered by %s",stat.getPath().getName(),filter);
                        continue;
                    }
                }
                SequenceFile.Reader reader = new SequenceFile.Reader(fs, stat.getPath(), conf);

                // CompositeValueWrapper value = new CompositeValueWrapper();
                if (!stat.isDir())
                {
                    SequenceFile.Writer copier = SequenceFile.createWriter(fs, conf, new Path(dest.toString() + "/"
                            + stat.getPath().getName()), reader.getKeyClass(), reader.getValueClass());
                    Writable key = (Writable) ReflectionUtils.newInstance(reader.getKeyClass(), conf);
                    Writable value = (Writable) ReflectionUtils.newInstance(reader.getValueClass(), conf);
                    while (reader.next(key, value))
                    {
                        copier.append(key, value);
                    }
                    copier.close();
                    reader.close();
                }
            }

        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void copyFiles(String srcDir, String destDir, PathFilter filter)
    {
        Path src = new Path(srcDir);
        Path dest = new Path(destDir);

        try
        {
            FileSystem fs = src.getFileSystem(conf);
            if (!fs.exists(src))
                return;
            for (FileStatus stat : fs.listStatus(src))
            {
                if (!filter.equals(empty))
                {
                    if (!filter.accept(stat.getPath()))
                    {
                        // System.out.printf("file %s filtered by %s",stat.getPath().getName(),filter);
                        continue;
                    }
                }
                SequenceFile.Reader reader = new SequenceFile.Reader(fs, stat.getPath(), conf);

                // CompositeValueWrapper value = new CompositeValueWrapper();
                if (!stat.isDir())
                {
                    SequenceFile.Writer copier = SequenceFile.createWriter(fs, conf, new Path(dest.toString() + "/"
                            + stat.getPath().getName()), reader.getKeyClass(), reader.getValueClass());
                    Writable key = (Writable) ReflectionUtils.newInstance(reader.getKeyClass(), conf);
                    Writable value = (Writable) ReflectionUtils.newInstance(reader.getValueClass(), conf);
                    while (reader.next(key, value))
                    {
                        copier.append(key, value);
                    }
                    copier.close();
                    reader.close();
                }
            }

        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static boolean exists(String pathstr)
    {
        Path path = new Path(pathstr);
        return exists(path);
    }

    public static boolean exists(Path path)
    {
        boolean result = false;
        try
        {
            FileSystem fs = path.getFileSystem(conf);
            result = fs.exists(path);
        } catch (IOException e)
        {

            //TucLogger.getInstance().log("Exception: HDFSUtils.exists " + path.toString(), TucLogger.ERROR);
            e.printStackTrace();

        }

        return result;

    }

    public static boolean isEmpty(String pathstr)
    {
        Path path = new Path(pathstr);
        return isEmpty(path);
    }

    public static boolean isEmpty(Path path)
    {
        boolean result = true;
        try
        {

            FileSystem fs = FileSystem.get(conf);
            SequenceFile.Reader reader = new SequenceFile.Reader(fs, path, conf);
            Writable key = (Writable) ReflectionUtils.newInstance(reader.getKeyClass(), conf);
            Writable value = (Writable) ReflectionUtils.newInstance(reader.getValueClass(), conf);

            if (reader.next(key, value))
                result = false;
            reader.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static boolean isDirectory(Path path)
    {
        FileSystem fs;
        try
        {
            fs = path.getFileSystem(conf);
            return !fs.isFile(path);
        } catch (IOException e)
        {
            //TucLogger.getInstance().log("Exception is Directory HDFSUtils", TucLogger.ERROR);
            e.printStackTrace();
        }
        return false;

    }

    public static void dump(Path input, Path output)
    {
        SequenceFile.Reader reader = null;
        FSDataOutputStream writer = null;
        if (HDFSUtils.isEmpty(input))
            return;
        try
        {

            FileSystem fs = FileSystem.get(conf);
            reader = new SequenceFile.Reader(fs, input, conf);
            Writable key = (Writable) ReflectionUtils.newInstance(reader.getKeyClass(), conf);
            Writable value = (Writable) ReflectionUtils.newInstance(reader.getValueClass(), conf);
            writer = fs.create(output);

            while (reader.next(key, value))
            {
                writer.writeBytes("key: " + key.toString() + "\n" + value.toString() + "\n");
            }
            reader.close();
            writer.close();
        } catch (IOException e)
        {

            e.printStackTrace();
        }
    }

    public static void dump(String input, String output)
    {
        Path inputPath = new Path(input);
        Path outputPath = new Path(output);
        HDFSUtils.dump(inputPath, outputPath);
    }

    public static void dump(Path input, String output)
    {
        Path outputPath = new Path(output);
        HDFSUtils.dump(input, outputPath);
    }

    public static Path[] listStatus(String path, String pattern)
    {
        Path p = new Path(path);
        return listStatus(p, pattern);
    }

    public static Path[] listStatus(Path path, String pattern)
    {
        Path[] result = null;
        FileStatus[] files = null;
        try
        {
            FileSystem fs = FileSystem.get(new Configuration());
            if (null != pattern && !pattern.equals(""))
                files = fs.listStatus(path, new StringPathFilter(pattern));
            else
                files = fs.listStatus(path);

        } catch (IOException e)
        {

            e.printStackTrace();
        }
        if (files == null)
            return null;
        result = new Path[files.length];
        int index = 0;
        for (FileStatus f : files)
            result[index++] = f.getPath();
        return result;
    }

    public static void dumpMap(Path input, Path output)
    {
        MapFile.Reader reader = null;
        FSDataOutputStream writer = null;
        try
        {
            Configuration conf = new Configuration();
            FileSystem fs = FileSystem.get(conf);
            reader = new MapFile.Reader(fs, input.toString(), conf);
            WritableComparable key = (WritableComparable) ReflectionUtils.newInstance(reader.getKeyClass(), conf);
            Writable value = (Writable) ReflectionUtils.newInstance(reader.getValueClass(), conf);
            writer = fs.create(output);

            while (reader.next(key, value))
            {
                writer.writeBytes("key: " + key.toString() + "\n" + value.toString() + "\n");
            }
            reader.close();
            writer.close();
        } catch (IOException e)
        {

            e.printStackTrace();
        }
    }

    public static void dumpMap(String input, String output)
    {
        Path inputPath = new Path(input);
        Path outputPath = new Path(output);
        HDFSUtils.dumpMap(inputPath, outputPath);
    }

    public static void dumpMap(Path input, String output)
    {
        Path outputPath = new Path(output);
        HDFSUtils.dumpMap(input, outputPath);
    }

    public static void mkdirs(String output)
    {
        Path outputPath = new Path(output);
        HDFSUtils.mkdirs(outputPath);
    }

    public static void mkdirs(Path outputPath)
    {

        try
        {
            FileSystem fs = outputPath.getFileSystem(conf);
            if (!fs.exists(outputPath))
                fs.mkdirs(outputPath);
        } catch (IOException e)
        {

            e.printStackTrace();
        }

    }

    public static MapFile.Reader getMapReader(String file) throws IOException
    {
        Path p = new Path(file);
        return HDFSUtils.getMapReader(p);
    }

    public static MapFile.Reader getMapReader(Path path) throws IOException
    {

        FileSystem fs = path.getFileSystem(conf);
        MapFile.Reader result = new MapFile.Reader(fs, HDFSUtils.makeQualified(path).toUri().toString(), conf);
        return result;
    }

    public static Reader getReader(String file) throws IOException
    {
        Path p = new Path(file);
        return HDFSUtils.getReader(p);
    }

    public static Reader getReader(Path path) throws IOException
    {

        FileSystem fs = path.getFileSystem(conf);
        SequenceFile.Reader result = new SequenceFile.Reader(fs, path, conf);
        return result;
    }

    public static FSDataOutputStream getOutputStream(String file, boolean overwrite) throws IOException
    {
        Path p = new Path(file);
        return HDFSUtils.getOutputStream(p, overwrite);
    }

    public static FSDataOutputStream getOutputStream(Path p, boolean overwrite) throws IOException
    {
        FileSystem fs = p.getFileSystem(conf);
        FSDataOutputStream result = null;
        if (fs.exists(p))
        {
            if (!overwrite)
                result = fs.append(p);
            else
                HDFSUtils.deletePath(HDFSUtils.makeQualified(p).toString(), true);
        } else
        {
            result = fs.create(p);
        }
        return result;
    }

    public static FSDataInputStream getInputStream(String file) throws IOException
    {
        Path p = new Path(file);
        return HDFSUtils.getInputStream(p);
    }

    private static FSDataInputStream getInputStream(Path p) throws IOException
    {

        FileSystem fs = p.getFileSystem(conf);
        FSDataInputStream result = fs.open(p);
        return result;
    }

    public static long getSize(String filename)
    {

        Path path = new Path(filename);
        return HDFSUtils.getSize(path);
    }

    private static long getSize(Path path)
    {
        long result = 0;
        try
        {

            FileSystem fs = path.getFileSystem(conf);

            FileStatus file = fs.getFileStatus(path);
            if (file.isDir())
            {
                FileStatus[] real_files = fs.listStatus(path);
                for (FileStatus f : real_files)
                {
                    result += f.getLen();
                }
            } else
            {
                result = file.getLen();
            }

        } catch (IOException e)
        {
            //TucLogger.getInstance().log(HDFSUtils.class.toString() + ".getSize( " + path.toString() + ") Exception",
             //       TucLogger.ERROR);
            e.printStackTrace();
        }

        return result;// /
        // GlobalConfiguration.getInstance().getLong("bepadoop.conf.MBSIZE");
    }

    public static String mergeFiles(Path[] input, String output) throws IOException
    {
        if (input == null || input.length == 0)
            return "";
        Path outputPath = new Path(output);
        FileSystem fs = outputPath.getFileSystem(conf);
        SequenceFile.Writer writer = null;

        for (Path file : input)
        {

            if (HDFSUtils.isEmpty(file))
            {
                HDFSUtils.deletePath(file.toUri().toString());
                continue;
            }

            SequenceFile.Reader reader = HDFSUtils.getReader(file);
            Writable key = (Writable) ReflectionUtils.newInstance(reader.getKeyClass(), conf);
            Writable value = (Writable) ReflectionUtils.newInstance(reader.getValueClass(), conf);
            if (writer == null)
                writer = SequenceFile.createWriter(fs, conf, outputPath, key.getClass(), value.getClass());
            while (reader.next(key, value))
            {
                writer.append(key, value);
            }
            reader.close();
        }
        if (writer != null)
            writer.close();

        return output;
    }

    public static String mergeFiles(String cliqueout, String string) throws IOException
    {
        Path p = new Path(cliqueout);
        Path o = new Path(string);
        return HDFSUtils.mergeFiles(p, o);
    }

    public static String mergeFiles(Path p, String o) throws IOException
    {
        Path output = new Path(o);
        return HDFSUtils.mergeFiles(p, output);

    }

    public static String mergeFiles(Path p, Path o) throws IOException
    {
        String outputFile = "";
        if (!HDFSUtils.exists(o))
        {
            outputFile = HDFSUtils.makeQualified(o).toUri().toString();
        } else
        {
            if (HDFSUtils.isDirectory(o))
            {
                outputFile = o.toUri().toString() + "/" + p.getName() + ".merged";
            } else
            {
                if (HDFSUtils.isEmpty(o))
                {
                    HDFSUtils.deletePath(o.toUri().toString());
                    outputFile = o.toUri().toString();
                } else
                {
                    outputFile = o.toUri().toString() + ".merged_from_" + p.getName();
                }
            }
        }
        if (!HDFSUtils.isDirectory(p))
        {
            HDFSUtils.copyFiles(p.toUri().toString(), outputFile, "");
        } else
        {

            Path[] files = HDFSUtils.listStatus(p, "");
            if (files == null || files.length == 0)
                return "";
            FileSystem fs = p.getFileSystem(conf);
            SequenceFile.Writer writer = null;
            Path outputPath = new Path(outputFile);
            for (Path file : files)
            {

                if (HDFSUtils.isEmpty(file))
                {
                    HDFSUtils.deletePath(file.toUri().toString());
                    continue;
                }

                SequenceFile.Reader reader = HDFSUtils.getReader(file);
                Writable key = (Writable) ReflectionUtils.newInstance(reader.getKeyClass(), conf);
                Writable value = (Writable) ReflectionUtils.newInstance(reader.getValueClass(), conf);
                if (writer == null)
                    writer = SequenceFile.createWriter(fs, conf, outputPath, key.getClass(), value.getClass());
                while (reader.next(key, value))
                {
                    writer.append(key, value);
                }
                reader.close();
            }
            if (writer != null)
                writer.close();

        }
        return outputFile;
    }

    public static Path makeQualified(String p) throws IOException
    {
        Path path = new Path(p);
        return HDFSUtils.makeQualified(path);
    }

    public static Path makeQualified(Path p) throws IOException
    {

        FileSystem fs = p.getFileSystem(conf);
        Path result = fs.makeQualified(p);
        return result;

    }

    public static Writer getWriter(String string, Class keyClass, Class valueClass) throws IOException
    {
        Path p = new Path(string);
        SequenceFile.Writer result = SequenceFile.createWriter(FileSystem.get(conf), conf, p, keyClass, valueClass);
        return result;
    }

}