#!/bin/bash

EXP_ID=$1
DIM_VALUE=$2
INSTANCES=$3
PARTITIONS=${4:-40}
CPU_LIMIT=${5:-1000m}
MEMORY_LIMIT=${6:-4Gi}
KAFKA_STREAMS_COMMIT_INTERVAL_MS=${7:-100}
EXECUTION_MINUTES=${8:-5}

echo "EXP_ID: $EXP_ID"
echo "DIM_VALUE: $DIM_VALUE"
echo "INSTANCES: $INSTANCES"
echo "PARTITIONS: $PARTITIONS"
echo "CPU_LIMIT: $CPU_LIMIT"
echo "MEMORY_LIMIT: $MEMORY_LIMIT"
echo "KAFKA_STREAMS_COMMIT_INTERVAL_MS: $KAFKA_STREAMS_COMMIT_INTERVAL_MS"
echo "EXECUTION_MINUTES: $EXECUTION_MINUTES"

# Create Topics
#PARTITIONS=40
#kubectl run temp-kafka --rm --attach --restart=Never --image=solsson/kafka --command -- bash -c "./bin/kafka-topics.sh --zookeeper my-confluent-cp-zookeeper:2181 --create --topic input --partitions $PARTITIONS --replication-factor 1; ./bin/kafka-topics.sh --zookeeper my-confluent-cp-zookeeper:2181 --create --topic configuration --partitions 1 --replication-factor 1; ./bin/kafka-topics.sh --zookeeper my-confluent-cp-zookeeper:2181 --create --topic output --partitions $PARTITIONS --replication-factor 1"
PARTITIONS=$PARTITIONS
kubectl exec kafka-client -- bash -c "kafka-topics --zookeeper my-confluent-cp-zookeeper:2181 --create --topic input --partitions $PARTITIONS --replication-factor 1; kafka-topics --zookeeper my-confluent-cp-zookeeper:2181 --create --topic aggregation-feedback --partitions $PARTITIONS --replication-factor 1; kafka-topics --zookeeper my-confluent-cp-zookeeper:2181 --create --topic configuration --partitions 1 --replication-factor 1; kafka-topics --zookeeper my-confluent-cp-zookeeper:2181 --create --topic output --partitions $PARTITIONS --replication-factor 1"

# Start workload generator
NUM_NESTED_GROUPS=$DIM_VALUE
WL_MAX_RECORDS=150000
APPROX_NUM_SENSORS=$((4**NUM_NESTED_GROUPS))
WL_INSTANCES=$(((APPROX_NUM_SENSORS + (WL_MAX_RECORDS -1 ))/ WL_MAX_RECORDS))

cat <<EOF >uc-workload-generator/overlay/uc2-workload-generator/set_paramters.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: titan-ccp-load-generator
spec:
  replicas: $WL_INSTANCES
  template:
    spec:
      containers:
      - name: workload-generator
        env:
        - name: NUM_SENSORS
          value: "4"
        - name: HIERARCHY
          value: "full"
        - name: NUM_NESTED_GROUPS
          value: "$NUM_NESTED_GROUPS"
        - name: INSTANCES
          value: "$WL_INSTANCES"
EOF
kubectl apply -k uc-workload-generator/overlay/uc2-workload-generator

# Start application
REPLICAS=$INSTANCES
cat <<EOF >uc-application/overlay/uc2-application/set_paramters.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: titan-ccp-aggregation
spec:
  replicas: $REPLICAS
  template:
    spec:
      containers:
      - name: uc-application
        env:
        - name: COMMIT_INTERVAL_MS
          value: "$KAFKA_STREAMS_COMMIT_INTERVAL_MS"
        resources:
          limits:
            memory: $MEMORY_LIMIT
            cpu: $CPU_LIMIT
EOF
kubectl apply -k uc-application/overlay/uc2-application

# Execute for certain time
sleep $(($EXECUTION_MINUTES * 60))

# Run eval script
source ../.venv/bin/activate
python lag_analysis.py $EXP_ID uc2 $DIM_VALUE $INSTANCES $EXECUTION_MINUTES
deactivate

# Stop workload generator and app
kubectl delete -k uc-workload-generator/overlay/uc2-workload-generator
kubectl delete -k uc-application/overlay/uc2-application


# Delete topics instead of Kafka
#kubectl exec kafka-client -- bash -c "kafka-topics --zookeeper my-confluent-cp-zookeeper:2181 --delete --topic 'input,output,configuration,titan-.*'"
# kafka-topics --zookeeper my-confluent-cp-zookeeper:2181 --delete --topic '.*'
#sleep 30s # TODO check
#kubectl exec kafka-client -- bash -c "kafka-topics --zookeeper my-confluent-cp-zookeeper:2181 --list" | sed -n '/^titan-.*/p;/^input$/p;/^output$/p;/^configuration$/p'
#kubectl exec kafka-client -- bash -c "kafka-topics --zookeeper my-confluent-cp-zookeeper:2181 --list" | sed -n '/^titan-.*/p;/^input$/p;/^output$/p;/^configuration$/p' | wc -l
#kubectl exec kafka-client -- bash -c "kafka-topics --zookeeper my-confluent-cp-zookeeper:2181 --list"

#kubectl exec kafka-client -- bash -c "kafka-topics --zookeeper my-confluent-cp-zookeeper:2181 --delete --topic 'input,output,configuration,titan-.*'"
echo "Finished execution, print topics:"
#kubectl exec kafka-client -- bash -c "kafka-topics --zookeeper my-confluent-cp-zookeeper:2181 --list" | sed -n -E '/^(titan-.*|input|output|configuration)( - marked for deletion)?$/p'
while test $(kubectl exec kafka-client -- bash -c "kafka-topics --zookeeper my-confluent-cp-zookeeper:2181 --list" | sed -n -E '/^(theodolite-.*|input|aggregation-feedback|output|configuration)( - marked for deletion)?$/p' | wc -l) -gt 0
do
    kubectl exec kafka-client -- bash -c "kafka-topics --zookeeper my-confluent-cp-zookeeper:2181 --delete --topic 'input|aggregation-feedback|output|configuration|theodolite-.*' --if-exists"
    echo "Wait for topic deletion"
    sleep 5s
    #echo "Finished waiting, print topics:"
    #kubectl exec kafka-client -- bash -c "kafka-topics --zookeeper my-confluent-cp-zookeeper:2181 --list" | sed -n -E '/^(titan-.*|input|output|configuration)( - marked for deletion)?$/p'
    # Sometimes a second deletion seems to be required
done
echo "Finish topic deletion, print topics:"
#kubectl exec kafka-client -- bash -c "kafka-topics --zookeeper my-confluent-cp-zookeeper:2181 --list" | sed -n -E '/^(titan-.*|input|output|configuration)( - marked for deletion)?$/p'

# delete zookeeper nodes used for workload generation
echo "Delete ZooKeeper configurations used for workload generation"
kubectl exec zookeeper-client -- bash -c "zookeeper-shell my-confluent-cp-zookeeper:2181 deleteall /workload-generation"
echo "Waiting for deletion"

while [ true ]
do
    IFS=', ' read -r -a array <<< $(kubectl exec zookeeper-client -- bash -c "zookeeper-shell my-confluent-cp-zookeeper:2181 ls /" | tail -n 1 | awk -F[\]\[] '{print $2}')
    found=0
    for element in "${array[@]}"
    do
        if [ "$element" == "workload-generation" ]; then
                found=1
                break
        fi
    done
    if [ $found -ne 1 ]; then
        echo "ZooKeeper reset was successful."
        break
    else
        echo "ZooKeeper reset was not successful. Retrying in 5s."
        sleep 5s
    fi
done
echo "Deletion finished"

echo "Exiting script"

KAFKA_LAG_EXPORTER_POD=$(kubectl get pod -l app.kubernetes.io/name=kafka-lag-exporter -o jsonpath="{.items[0].metadata.name}")
kubectl delete pod $KAFKA_LAG_EXPORTER_POD
