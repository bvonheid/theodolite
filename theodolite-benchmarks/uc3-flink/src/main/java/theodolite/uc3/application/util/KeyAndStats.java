package theodolite.uc3.application.util;

import com.google.common.math.Stats;

/**
 * Contains a key and stats object.
 */
public class KeyAndStats {
  private final String key;
  private final Stats stats;

  /**
   * Create an object with a key string and a {@link Stats} object.
   *
   * @param key string
   * @param stats object
   */
  public KeyAndStats(final String key, final Stats stats) {
    super();
    this.key = key;
    this.stats = stats;
  }


  /**
   * Returns the key.
   *
   * @return key string
   */
  public String getKey() {
    return this.key;
  }


  /**
   * Returns the {@link Stats} object.
   *
   * @return {@link Stats} object
   */
  public Stats getStats() {
    return this.stats;
  }


}
