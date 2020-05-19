package common.generators;

/**
 * Base methods for workload generators.
 */
public interface IWorkloadGenerator {

  /**
   * Start the workload generation.
   */
  void start();

  /**
   * Stop the workload generation.
   */
  void stop();

}
