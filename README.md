# Java 内网穿透（A/B/C 三机）示例

这个示例实现了你描述的场景：

- **B**：云主机，部署转发服务（`TunnelServer`）
- **A**：家庭局域网设备，运行代理（`TunnelAgent`），把本地服务暴露到隧道
- **C**：另一个局域网设备，运行访问端（`TunnelVisitor`），通过 B 访问 A 的服务

## 架构

- `control` 通道（A -> B）：A 注册隧道，并监听 B 的连接指令
- `relay` 通道（C -> B）：C 请求访问某个 `tunnelId`
- `data` 通道（A -> B）：当 C 发起访问时，A 回连 B 建立数据流

B 将 C 和 A 的数据连接配对后做双向转发，实现跨 NAT 通信。

## 代码结构

- `TunnelServer`：部署在 B，负责注册、会话调度、转发
- `TunnelAgent`：部署在 A，负责把本地目标服务接入隧道
- `TunnelVisitor`：部署在 C，在本地起监听端口，转发到 A
- `RelayUtil`：双向字节流转发工具

## 构建

```bash
mvn -q -DskipTests package
```

## 启动方式

示例端口：
- control: `7000`
- relay: `7001`
- data: `7002`
- tunnelId: `home-ssh`

### 1) B（云主机）

```bash
java -cp target/intranet-tunnel-1.0.0.jar com.manydemo.tunnel.TunnelServer 7000 7001 7002
```

### 2) A（内网设备）

假设 A 要暴露本地 SSH（`127.0.0.1:22`）：

```bash
java -cp target/intranet-tunnel-1.0.0.jar com.manydemo.tunnel.TunnelAgent <B_IP> 7000 7002 home-ssh 127.0.0.1 22
```

### 3) C（访问端）

在 C 本机监听 `10022`，转发到 A 的 SSH：

```bash
java -cp target/intranet-tunnel-1.0.0.jar com.manydemo.tunnel.TunnelVisitor <B_IP> 7001 home-ssh 10022
```

然后在 C 上直接访问：

```bash
ssh -p 10022 user@127.0.0.1
```

## 注意事项

1. B 需要放行 7000/7001/7002 端口。
2. 这是教学级实现，生产建议补充：
   - TLS 加密与双向认证
   - 心跳与重连机制
   - 会话超时和流控
   - 多租户鉴权与审计日志
3. 当前协议是简化文本协议（`HELLO/OPEN/CONNECT/DATA`）。
