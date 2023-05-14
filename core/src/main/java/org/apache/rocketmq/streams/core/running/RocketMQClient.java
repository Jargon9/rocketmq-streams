package org.apache.rocketmq.streams.core.running;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import static org.apache.rocketmq.common.protocol.heartbeat.SubscriptionData.SUB_ALL;
import static org.apache.rocketmq.streams.core.common.Constant.*;
import static org.apache.rocketmq.streams.core.metadata.StreamConfig.ROCKETMQ_STREAMS_CONSUMER_FORM_WHERE;

public class RocketMQClient {
    private static final Logger logger = LoggerFactory.getLogger(RocketMQClient.class);
    private final String nameSrvAddr;

    public RocketMQClient(String nameSrvAddr) {
        this.nameSrvAddr = nameSrvAddr;
    }

    public DefaultLitePullConsumer pullConsumer(String groupName,
                                                Set<String> topics,
                                                ConsumeFromWhere consumeFromWhere) throws MQClientException {
        DefaultLitePullConsumer pullConsumer = new DefaultLitePullConsumer(groupName);
        pullConsumer.setNamesrvAddr(nameSrvAddr);
        pullConsumer.setAutoCommit(false);
        pullConsumer.setPullBatchSize(1000);

        for (String topic : topics) {
            Collection<MessageQueue> messageQueues = pullConsumer.fetchMessageQueues(topic);
            if (consumeFromWhere.equals(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET)
                    && !topic.endsWith(SHUFFLE_TOPIC_SUFFIX) && !topic.endsWith(STATE_TOPIC_SUFFIX)) {
                messageQueues.forEach(messageQueue -> {
                    try {
                        pullConsumer.seek(messageQueue, FIRST_OFFSET);
                    } catch (MQClientException e) {
                        logger.error("reset messageQueue:{} consumer offset to zero failed.", messageQueue);
                    }
                });
            }
            pullConsumer.subscribe(topic, SUB_ALL);
            logger.debug("subscribe topic:{}, groupName:{}", topic, groupName);
        }

        return pullConsumer;
    }

    public DefaultMQProducer producer(String groupName) {
        DefaultMQProducer producer = new DefaultMQProducer(groupName);
        producer.setNamesrvAddr(nameSrvAddr);
        return producer;
    }

    public DefaultMQAdminExt getMQAdmin() throws MQClientException {
        DefaultMQAdminExt mqAdminExt = new DefaultMQAdminExt(1000);
        mqAdminExt.setInstanceName(UUID.randomUUID().toString());
        mqAdminExt.setNamesrvAddr(nameSrvAddr);
        mqAdminExt.start();
        return mqAdminExt;
    }
}
