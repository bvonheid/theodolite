# Theodolite Helm Chart

## Installation

Install the chart via:

```sh
helm dependencies update .
helm install my-confluent .
```

**Please note: Theodolite currently uses hard-coded URLs, to connect to Kafka and Zookeeper. For that reason, the name of this chart must be `my-confluent`.** We will change this behavior soon.

This chart installs requirements to execute benchmarks with Theodolite.

Dependencies and subcharts:

- Prometheus Operator
- Prometheus
- Grafana (incl. dashboard and data source configuration)
- Kafka
- Zookeeper
- A Kafka client pod

## Test

Test the installation:

```sh
helm test <release-name>
```

Our test files are located [here](templates/../../theodolite-chart/templates/tests). Many subcharts have their own tests, these are also executed and are placed in the respective /templates folders. 

Please note: If a test fails, Helm will stop testing.

It is possible that the tests are not running successfully at the moment. This is because the Helm tests of the subchart cp-confluent receive a timeout exception. There is an [issue](https://github.com/confluentinc/cp-helm-charts/issues/318) for this problem on GitHub.

## Configuration

In development environments Kubernetes resources are often low. To reduce resource consumption, we provide an `one-broker-value.yaml` file. This file can be used with:

```sh
helm install theodolite . -f preconfigs/one-broker-values.yaml
```

## Development

**Hints**:

- Grafana configuration: Grafana ConfigMaps contains expressions like {{ topic }}. Helm uses the same syntax for template function. More information [here](https://github.com/helm/helm/issues/2798)
  - Escape braces: {{ "{{" topic }}
  - Let Helm render the template as raw string: {{ `{{ <config>}}` }}
  