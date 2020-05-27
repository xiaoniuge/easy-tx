package icu.funkye.easy.tx;

import icu.funkye.easy.tx.listener.EasyMQConsumeMsgListenerProcessor;
import icu.funkye.easy.tx.properties.RocketMqProperties;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author 陈健斌 funkye
 */
@ComponentScan(basePackages = {"icu.funkye.easy.tx.config", "icu.funkye.easy.tx.listener", "icu.funkye.easy.tx.aspect",
    "icu.funkye.easy.tx.http", "icu.funkye.easy.tx.feign", "icu.funkye.easy.tx.properties"})
@EnableConfigurationProperties({RocketMqProperties.class})
@Configuration
public class EasyTxAutoConfigure {

    @Autowired
    private RocketMqProperties prop;

    @Autowired
    private EasyMQConsumeMsgListenerProcessor consumeMsgListenerProcessor;

    private static final Logger LOGGER = LoggerFactory.getLogger(EasyTxAutoConfigure.class);

    @Bean
    public DefaultMQProducer easyTxProducer() throws MQClientException {
        LOGGER.info("defaultProducer 正在创建---------------------------------------");
        DefaultMQProducer producer = new DefaultMQProducer(prop.getGroup());
        producer.setNamesrvAddr(prop.getNameServer());
        producer.setVipChannelEnabled(false);
        producer.start();
        LOGGER.info("rocketmq producer server 开启成功----------------------------------");
        return producer;
    }

    @Bean
    public DefaultMQPushConsumer easyTxConsumer() throws MQClientException {
        LOGGER.info("defaultConsumer 正在创建---------------------------------------");
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(prop.getGroup());
        consumer.setNamesrvAddr(prop.getNameServer());
        // 设置监听
        consumer.registerMessageListener(consumeMsgListenerProcessor);

        /**
         * 设置consumer第一次启动是从队列头部开始还是队列尾部开始 如果不是第一次启动，那么按照上次消费的位置继续消费
         */
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);

        /**
         * 设置消费模型，集群还是广播，默认为集群
         */
        consumer.setMessageModel(MessageModel.BROADCASTING);

        try {
            consumer.setVipChannelEnabled(false);
            consumer.subscribe(prop.getTopic(), "*");
            consumer.start();
            LOGGER.info("consumer 创建成功 groupName={}, topics={}, namesrvAddr={}", prop.getGroup(), prop.getTopic(),
                prop.getNameServer());
        } catch (MQClientException e) {
            LOGGER.error("consumer 创建失败!");
        }
        return consumer;
    }
}
