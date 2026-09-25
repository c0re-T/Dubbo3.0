# Dubbo 3.0 远程调用与流式通信实验

一个用于学习 **Apache Dubbo 3** 的多模块 Maven 项目，按提交顺序逐步演进：先用 HTTP（`RestTemplate`）让 consumer 调用 provider，再替换为 **ZooKeeper 注册中心 + Dubbo 远程调用**，最后在 `tri` 协议上验证 Dubbo 3 的 **服务端流 / 客户端流（双向流）**。

> 这是一个实验/学习项目，代码以"能看清调用链路"为目的，不是可直接上生产的工程实践。

## 技术栈

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| Java | 21 | `java.version` 与三个模块的 `maven.compiler.source/target` 均为 21 |
| Spring Boot | 3.5.8 | 父 POM 继承 `spring-boot-starter-parent` |
| Apache Dubbo | 3.3.6 | `dubbo-spring-boot-starter` + `dubbo-rpc-dubbo` + `dubbo-rpc-triple` + `dubbo-registry-zookeeper` |
| ZooKeeper | 3.8.x | 注册中心，本地 standalone 单节点 `127.0.0.1:2181` |
| Lombok | 1.18.42 | 仅用于 `@RequiredArgsConstructor` |

## 模块结构

```
Dubbo3.0/
├── pom.xml                    # 聚合父工程（packaging = pom），统一依赖版本
├── common/                    # 服务契约：只有接口，被 provider / consumer 共同依赖
│   └── com.ittxf.common.UserService
├── provider/                  # 服务提供方，HTTP 8081 / Dubbo tri 20880
│   ├── com.ittxf.provider.controller.UserController
│   └── com.ittxf.provider.service.impl.{UserServiceImpl, UserServiceImpl2}
└── consumer/                  # 服务消费方，HTTP 8082
    ├── com.ittxf.consumer.controller.OrderController
    └── com.ittxf.consumer.service.OrderService
```

`common` 只依赖 `dubbo-common`（为了 `StreamObserver`），不引入 Spring，避免契约模块被实现细节污染。

## 调用链路

```
浏览器 / curl
   │  GET http://localhost:8082/order
   ▼
consumer (8082)  OrderController → OrderService
   │  @DubboReference(version = "1.0")  ← 从 ZooKeeper 订阅
   ▼
ZooKeeper (2181) 注册中心：服务发现
   │
   ▼
provider (20880, tri 协议)  UserServiceImpl (version = "1.0")

另一条：GET http://localhost:8081/user
provider 内部同样通过 @DubboReference 引用自己发布的 UserService，
用于观察"一次 HTTP 请求如何变成一次 Dubbo 调用"。
```

## 已验证的通信模式

`UserService` 用 `default` 方法把三种模式并列声明，切换调用时只改 consumer 侧代码：

```java
public interface UserService {
    String getUser();                                                  // UNARY
    default void sayHelloServerStream(String name, StreamObserver<String> response) {}   // SERVER_STREAM
    default StreamObserver<String> sayHelloClientStream(StreamObserver<String> response) { return response; } // CLIENT_STREAM / BI_STREAM
}
```

- **UNARY**：`getUser()` 返回 `User Service version 1.0`。
- **SERVER_STREAM**：入参是普通值 + `StreamObserver` 回调，服务端连续 `onNext` 两次再 `onCompleted()`。
- **CLIENT_STREAM / BI_STREAM**：`sayHelloClientStream` 返回一个 `StreamObserver`，consumer 用它连续发送 `1`、`2`、`3`；服务端在返回的 observer 里接收并回写响应。
- **版本路由**：`UserServiceImpl` 发布 `version = "1.0"`，`UserServiceImpl2` 发布 `version = "2.0"`（只实现 `getUser`），consumer 用 `@DubboReference(version = "1.0")` 精确选中 1.0。改注解里的 version 即可切换实例。

流式通信依赖 `dubbo.protocol.name: tri`（gRPC 兼容协议）；原生 `dubbo` 协议不支持这三种流式模式。

## 快速开始

### 1. 启动 ZooKeeper

本地已装 ZooKeeper 时，直接启动它的 `zkServer`（Windows 下 `bin\zkServer.cmd`）即可，默认监听 2181。

### 2. 编译打包

```bash
mvn clean package -DskipTests
```

### 3. 启动两个应用

必须先起 provider，再起 consumer（consumer 启动时要从注册中心拉取提供者地址）。

```bash
java -jar provider/target/provider-0.0.1-SNAPSHOT.jar
java -jar consumer/target/consumer-0.0.1-SNAPSHOT.jar
```

在 IDE 中分别运行 `ProviderApplication` / `ConsumerApplication` 的 `main` 方法效果相同。

`spring-boot-maven-plugin` 只声明在 `provider` 和 `consumer` 两个模块里，`common` 不打包成可执行 jar（详见下方"踩过的坑"）。

### 4. 端口一览

| 端口 | 归属 |
| --- | --- |
| 2181 | ZooKeeper |
| 20880 | provider 的 Dubbo `tri` 协议端口 |
| 8081 | provider 的 HTTP |
| 8082 | consumer 的 HTTP |
| 22222 | Dubbo QoS —— **两侧配置里都已关闭** |

## 验证

以下输出是在本机（JDK 21 + ZooKeeper 2181）实际跑通后记录的。

```bash
curl http://localhost:8082/order
# → Order Service:User Service version 1.0

curl http://localhost:8081/user
# → User Service version 1.0
```

`/order` 会先发起一次客户端流（发送 `1`、`2`、`3`），再调用 `getUser()`。**流式结果打印在控制台，不在 HTTP 响应体里**——consumer 侧的 `StreamObserver.onNext` 只做了 `System.out.println`，响应体内容来自末尾那次 UNARY 调用。

consumer 控制台：

```
接受到结果：响应结果:1
接受到结果：hello:1
接受到结果：响应结果:2
接受到结果：hello:2
接受到结果：响应结果:3
接受到结果：hello:3
```

provider 控制台：

```
接受到结果：1
接受到结果：2
接受到结果：3
服务端处理完成
```

### 查看注册中心

不部署 Dubbo Admin 也能确认注册情况：

```bash
echo "ls /services" | <ZOOKEEPER_HOME>/bin/zkCli.cmd -server 127.0.0.1:2181
echo "ls /dubbo/com.ittxf.common.UserService/providers" | <ZOOKEEPER_HOME>/bin/zkCli.cmd -server 127.0.0.1:2181
```

实测结果：

```
/services                                        → [provider]
/services/provider                               → [<本机IP>:20880]
/dubbo/com.ittxf.common.UserService/providers    → []        # 空的
```

provider 配置了 `dubbo.application.register-mode: instance`，即只做**应用级（实例级）注册**，所以实例信息写在 `/services/<应用名>` 下；接口级路径虽然存在，但 `providers` 子节点是空的。consumer 只订阅不注册，因此 `/services` 下看不到它。想同时看到接口级节点，把该项改成 `all` 或 `interface`（对应 Dubbo 3 从接口级向应用级迁移的双注册策略）。

## 踩过的坑

- **QoS 端口冲突**：consumer 的 `application.yml` 里 `qos-enable: false` 是必须的，否则两个进程都想占 22222，后者启动失败。
- **`spring-boot-maven-plugin` 不能放在父 POM 的 `<build><plugins>`**：那样它会作用到所有模块，`common` 没有 main 方法，`repackage` 直接报 `Unable to find main class`，`mvn clean package` 整个断在这里。现在插件只声明在 provider / consumer 里。
- **`response.onCompleted()` 不能写在 `onNext()` 里**：服务端流的完成信号一旦发出就不能再写。最初的写法每条请求都顺手 `onCompleted()`，结果只有第一条 `"1"` 的响应送到了 consumer，服务端处理 `"2"` 时抛 `DecodeException: Unexpected serialization type:null`，`"3"` 完全没被处理。把 `onCompleted()` 移到请求流的 `onCompleted()` 回调里之后，三条请求的六条响应才全部到达。
- **流式模式没反应**：先检查 `dubbo.protocol.name` 是不是 `tri`（原生 `dubbo` 协议不支持流式），以及 provider 侧是否真的 override 了那些 `default` 方法——接口里的 `default` 实现是空方法体，忘了实现不会报错，只会静默无输出。
- **`UserServiceImpl2` 没实现流式方法**：它只覆盖了 `getUser()`，流式调用会落到接口的 `default` 空实现上。这是有意的：它用来演示 `version` 路由，不用来测流式。
- **provider 上的 `UserController` 是自己调自己**：容易误判为"跨服务调用"。它只是为了把 HTTP → Dubbo 这一步单独拎出来观察。
- **`RestTemplateConfig` 里的 Bean 全被注释了**：第一个阶段（HTTP 直连调用）的残留，切到 Dubbo 后不再需要，保留文件只是为了对照演进过程。

## Git 演进记录

| 提交 | 内容 |
| --- | --- |
| `10d1f53` | 基础框架搭建 |
| `7dfd6dd` | consumer 通过 HTTP 调用 provider |
| `95f32e1` | 改为 ZooKeeper + Dubbo 做服务发现与远程调用，拆出 `common` 契约模块，加入 `version` 路由（`UserServiceImpl2`） |
| `0828e65` | 修复启动异常：应用名改用 `dubbo.application.name`，并关闭 QoS 端口 |
| `cba2fd1` | 协议由 `dubbo` 切到 `tri`，加入服务端流式返回与 `register-mode: instance` |
