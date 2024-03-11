---
title: kafka笔记
date: 2024-03-11 18:17:22
lang: zh-cn
tags: 
---

# kafka笔记

### 一、发送消息

代码示例：

```java
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;  

    @Resource
    KafkaTemplate<String, String> template;

  
    public String sendKafkaMsg(String topic, Integer partition, String key, String value) throws ExecutionException, InterruptedException {
        ListenableFuture<SendResult<String,String>> result =  template.send(topic, partition, System.currentTimeMillis(), key, value);
        SendResult<String, String> sendResult = result.get();
        String s = sendResult.toString();
        return s;
    }
```

发送消息可以指定参数：**​ topic,  partition,  key,  value**

**topic：**消息主题

**patition,**分区，一个分区就是一个队列，维护自己的offset，指定了partition则消息发送到对应partition中；  
**key**，分区key，如果不指定key也不指定partition，则采用轮询方式发送到不同patition；如果指定key没有指定partition，则通过key的hash/topic的partition数计算需要发送到哪个patition  
详细以下方法org.apache.kafka.clients.producer.internals.DefaultPartitioner#partition(java.lang.String, java.lang.Object, byte[], java.lang.Object, byte[], org.apache.kafka.common.Cluster, int)

```java
/**
     * Compute the partition for the given record.
     *
     * @param topic The topic name
     * @param numPartitions The number of partitions of the given {@code topic}
     * @param key The key to partition on (or null if no key)
     * @param keyBytes serialized key to partition on (or null if no key)
     * @param value The value to partition on or null
     * @param valueBytes serialized value to partition on or null
     * @param cluster The current cluster metadata
     */
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster,
                         int numPartitions) {
        if (keyBytes == null) {
            return stickyPartitionCache.partition(topic, cluster);
        }
        // hash the keyBytes to choose a partition
        return Utils.toPositive(Utils.murmur2(keyBytes)) % numPartitions;
    }
```

发送消息`org.springframework.kafka.core.KafkaTemplate`​：

```java
 /**
     * Creates a record to be sent to a specified topic and partition
     *
     * @param topic The topic the record will be appended to
     * @param partition The partition to which the record should be sent
     * @param key The key that will be included in the record
     * @param value The record contents
     */
public ListenableFuture<SendResult<K, V>> send(String topic, Integer partition, K key, @Nullable V data) {
		ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topic, partition, key, data);
		return doSend(producerRecord);
	}
```

### 二、接收消息

代码示例：

```java
    // 指定监听多个partition
    @KafkaListener(groupId = "group0",topicPartitions = {@TopicPartition(topic = "live-test", partitions = {"0","1"})})
    public void accept0(ConsumerRecord<?, ?> record) {
        log.info("收到消息：{}", record.toString());
    }
    // 不指定partition
    @KafkaListener(topics = "live-test",groupId = "group0")
    public void accept0(ConsumerRecord<?, ?> record) {
        log.info("收到消息：{}", record.toString());
    }
```

接收消息@KafkaListener可以指定参数：  
**topics** 数组形式  
**topicPattern**  字符串形式  
**topicPartitions**（可以指定具体的partition和offset）  
以上参数三选一  
**id**,此侦听器的容器的唯一标识符。如果没有指定，则使用自动生成的id。  
**idIsGroup**,当未提供groupId时，使用id (如果提供) 作为使用者的group.id属性  
**groupId**, 仅此侦听器使用此值覆盖消费者工厂的group.id属性

多个消费者订阅同一个topic的相同partition，则可以实现广播消息，各消费者都可以处理消息；  
多个消费者订阅同一个topic，不指定partition，则只有一个消费者收到消息；

多个消费者订阅同一个topic, groupId不同时，则可以实现广播消息，各消费者都可以处理消息；  
多个消费者订阅同一个topic, groupId同时，则只有一个消费者收到消息；

**每个partition的数据只能由group组中一个消费者消费**

**一个消费者可以订阅多个partition，例：**  
@KafkaListener(groupId = "group1",topicPartitions = {@TopicPartition(topic = "live-test", partitions = {"1","2"})})

‍

# 三、kafka 重平衡

#### 重平衡 Rebalance

* 重平衡rebalance本质上是一组协议，规定了一个消费者组ConsumerGroup下的所有消费者consumer如何达成一致来分配订阅队列topic的每个分区partition
* 上面介绍消费者与分区时，G1增加了消费者，kafka就会重新分配每个分区和消费者的对应关系。这个分配的过程就叫rebalance

#### Rebalance触发时机

1. 分区partition个数的增加，手动新增队列的分区
2. 对Topic的订阅发生变化，使用正则订阅topic，新增的topic符合正则规则
3. ConsumerGroup组成员发生变更（常见问题）

* 新的Consumer入组
* 已有Consumer离开组或者崩溃
* 消费过慢，导致kafka以为Consumer挂了，导致重平衡（重点问题）

‍

##### 重复消费出现情况

在配置自动提交enable.auto.commit 默认值true情况下，出现重复消费的场景有以下几种：

* Consumer 在消费过程中，应用进程被强制kill掉或发生异常退出。
* 消费者消费时间过长

##### 如何避免重复消费？

答：

1、第一种思路是提高消费能力，提高单条消息的处理速度，例如对消息处理中比 较耗时的步骤可通过异步的方式进行处理、利用多线程处理等。在缩短单条消息消费时常的同时，根据实际场景可将max.poll.interval.ms值设置大一点，避免不 必要的rebalance，此外可适当减小max.poll.records的值，默认值是500，可根 据实际消息速率适当调小。这种思路可解决因消费时间过长导致的重复消费问题， 对代码改动较小，但无法绝对避免重复消费问题。

2、第二种思路是引入单独去重机制，例如生成消息时，在消息中加入唯一标识符如消息id等。在消费端，我们可以保存最近的1000条消息id到redis或mysql表中，配置max.poll.records的值小于1000。在消费消息时先通过前置表去重后再进行消息的处理。如将topic + partition + offset进行原子性存储，已消费的记录下来

3、此外，在一些消费场景中，我们可以将消费的接口幂等处理，例如数据库的查 询操作天然具有幂等性，这时候可不用考虑重复消费的问题。对于例如新增数据的操作，可通过设置唯一键等方式以达到单次与多次操作对系统的影响相同，从而使接口具有幂等性。

‍
