# Java 内网穿透（A/B/C：A、C 都是客户端，B 是服务端）

按你的要求，当前方案为：

- **B**：云主机，仅部署服务端 `TunnelServer`
- **A、C**：都部署同一个客户端程序 `TunnelClient`
- 客户端可以**主动发起到另一台客户端**的请求（双向对等）

---

## 一、通信模型

- 每个客户端（A/C）都通过 `control` 长连接向 B 注册自己的 `clientId`
- 某客户端本地有连接进来时（例如本机访问 `127.0.0.1:10022`），它会通过 `relay` 向 B 发 `OPEN <targetClientId>`
- B 通知目标客户端回连 `data` 通道
- B 将“发起方连接”和“目标方回连连接”配对后做双向字节转发

这样 A 可以请求 C，C 也可以请求 A（只要互相把 `targetClientId` 配置为对方）。

---

## 二、代码结构

- `TunnelServer`（部署在 B）
  - 管理在线客户端
  - 处理 `OPEN` 请求并通知目标客户端
  - 按 `sessionId` 配对并转发数据流
- `TunnelClient`（部署在 A/C）
  - 注册 `clientId`
  - 被动处理来自其他客户端的访问（连接本地暴露服务）
  - 主动把本地请求转发到目标客户端
- `RelayUtil`
  - 双向 socket 流转发工具

---

## 三、构建

```bash
mvn -q -DskipTests package
```

> 若你的环境无法访问 Maven 中央仓库，可先用 `javac` 直接编译验证。

---

## 四、启动示例

示例端口：
- control: `7000`
- relay: `7001`
- data: `7002`

### 1) B（云主机）启动服务端

```bash
java -cp target/intranet-tunnel-1.0.0.jar com.manydemo.tunnel.TunnelServer 7000 7001 7002
```

### 2) A（客户端）

假设 A 暴露本地 SSH（`127.0.0.1:22`），并希望主动访问 C 的服务：

```bash
java -cp target/intranet-tunnel-1.0.0.jar com.manydemo.tunnel.TunnelClient <B_IP> 7000 7001 7002 A C 127.0.0.1 22 10022
```

含义：
- `clientId=A`
- `targetClientId=C`
- 被访问时连接本地 `127.0.0.1:22`
- 本机监听 `10022`，本机连这个端口就是“主动请求 C”

### 3) C（客户端）

假设 C 暴露本地 SSH（`127.0.0.1:22`），并希望主动访问 A：

```bash
java -cp target/intranet-tunnel-1.0.0.jar com.manydemo.tunnel.TunnelClient <B_IP> 7000 7001 7002 C A 127.0.0.1 22 10023
```

---

## 五、如何使用

- 在 A 上访问 `127.0.0.1:10022`，即会通过 B 转发到 C 的暴露服务。
- 在 C 上访问 `127.0.0.1:10023`，即会通过 B 转发到 A 的暴露服务。

例如（在 A 上）：

```bash
ssh -p 10022 user@127.0.0.1
```

---

## 六、生产建议

当前实现用于演示，生产建议补充：
1. TLS 与双向认证
2. 自动重连与心跳保活
3. 会话超时、限流、最大并发控制
4. 客户端鉴权、访问控制列表（ACL）与审计日志
5. 会话状态清理和异常恢复
