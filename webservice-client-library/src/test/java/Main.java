//package berkeleydb.gettingStarted;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

/**
 * Created by ap0n on 11/20/14.
 */
public class Main {

  private static Random random = new Random();
  private static int d = 4;
  private static int w = 4;
  private static String dataDirectory = "/home/vagvaz/tmp2/af";

  public static void main(String[] args) throws IOException {
    File datasetDirectory = new File(dataDirectory);
    File[] files = datasetDirectory.listFiles();
    int[][] sketch = new int[d][w];

    for (File file : files) {

      BufferedReader bufferedReader =
          new BufferedReader(new InputStreamReader(new FileInputStream(file)));
      String line = "";
      while ((line = bufferedReader.readLine()) != null) {

        for (String word : line.split(" ")) {
          if (word == null || word.length() == 0) {
            continue;
          }
          int[] yDim = hashRandom(word.hashCode());
          if (yDim[0] == 2 && yDim[1] == 3 && yDim[2] == 0 && yDim[3] == 2) {
            System.out.println(word);
          }
          for (int i = 0; i < d; i++) {
            sketch[i][yDim[i]]++;
          }
        }
      }

      for (int x = 0; x < d; x++) {
        for (int y = 0; y < w; y++) {
          System.out.print(sketch[x][y] + ",");
        }
        System.out.println();
      }
    }
  }

  private static int[] hashRandom(int seed) {
    int[] hash = new int[d];
    random.setSeed(seed);
    for (int i = 0; i < d; i++) {
      hash[i] = random.nextInt(w);
    }
    return hash;
  }
}
