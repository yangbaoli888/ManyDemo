# 新生儿护理记录（移动端 Web）

一个基于 Java Spring Boot 的轻量项目，用于记录：

- 大便（仅时间）
- 小便（仅时间）
- 奶粉（时间 + 毫升，下拉选择，30 的倍数）
- 母乳（时间 + 时长）

并支持按日期查看每次打卡时间，使用不同图标区分四种类型。

> 数据会持久化到 `data/events.json`，服务停止后重启不会丢失打卡记录。

## 一键预览

```bash
./preview.sh
```

默认访问：`http://localhost:8080`

可选参数：

- `--port=8090` 自定义端口
- `--dry-run` 只打印启动命令，不实际启动

## 启动

```bash
mvn spring-boot:run
```

打开浏览器访问：`http://localhost:8080`
