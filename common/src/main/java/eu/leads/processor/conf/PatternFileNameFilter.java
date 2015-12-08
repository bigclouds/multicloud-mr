package eu.leads.processor.conf;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by vagvaz on 6/1/14.
 */
public class PatternFileNameFilter implements FilenameFilter {
  private String pattern;

  public PatternFileNameFilter(String pattern) {
    this.pattern = pattern;
  }

  /**
   * {@inheritDoc}
   */
  @Override public boolean accept(File dir, String name) {

    if (pattern.contains("*")) {
      String[] fixes = pattern.split("\\*");
      if (name.toString().startsWith(fixes[0]) && name.toString().endsWith(fixes[1])) {
        return true;
      } else {
        return false;
      }
    }
    return name.matches(pattern);
  }
}
