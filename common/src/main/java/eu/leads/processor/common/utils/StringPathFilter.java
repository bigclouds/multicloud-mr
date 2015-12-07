package eu.leads.processor.common.utils;

/**
 * Created by tr on 19/11/2014.
 */
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

public class StringPathFilter implements PathFilter
{
    private String str;

    public StringPathFilter(String filter)
    {
        str = filter;
    }

    @Override
    public boolean accept(Path path)
    {
        // TODO Auto-generated method stub
        if (path.getName().startsWith(str))
        {
            System.out.println("accepting " + path.getName());
            return true;
        }
        // System.out.println("Filtering " + path.getName());
        return false;
    }

}