## SQS Listener using Spring Cloud AWS

This is a simplified reference implementation to demonstrate the use of Spring Cloud AWS for SQS listener.

## Features

- Basic usage of the Spring Cloud AWS for SQS listener. see [docs](https://docs.awspring.io/spring-cloud-aws/docs/3.1.0/reference/html/index.html#sqs-integration) for advance usage.
- Demonstrate the testing using [ElasticMQ](https://github.com/softwaremill/elasticmq) embedded emulator.
- Demonstrate the usage of [Awaitility](https://github.com/awaitility/awaitility) for asserting the asynchronous message receipt, without using `Thread.sleep()`
- Demonstrate the asserting of async message receipt/processing using the log statement.

## Key principles

- No custom libraries or framework
- Use open source libraries in vanilla flavor, keep the learning curve, upgrades easy.


## Running the tests


https://docs.awspring.io/spring-cloud-aws/docs/3.1.0/reference/html/index.html#defaultcredentialsprovider

```
export AWS_ACCESS_KEY_ID=dummy          
export AWS_SECRET_ACCESS_KEY=dummy

mvn test -DTest=ApplicationTests.java
```

## Note

You will see exception like below after the tests execution. You can ignore this as it is thrown by the AWS SQS client upon shutdown of the ElasticMQ REST server.

If there is a graceful way, please let me know by raising an issue.

```
[event-app] [tyEventLoop-0-3] s.a.a.h.n.n.i.Http1TunnelConnectionPool  : [Channel: 0d3f4815] Unable to establish tunnel for channel 0d3f4815

java.io.IOException: Could not connect to proxy
        at software.amazon.awssdk.http.nio.netty.internal.ProxyTunnelInitHandler.channelRead(ProxyTunnelInitHandler.java:111) ~[netty-nio-client-2.25.16.jar:na]
        at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:442) ~[netty-transport-4.1.107.Final.jar:4.1.107.Final]
        at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:420) ~[netty-transport-4.1.107.Final.jar:4.1.107.Final]
```



