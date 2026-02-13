# TOTP 二维码生成器 Demo

一个零后端的前端示例，用于生成 **6 位数字** 的 TOTP 配置二维码（`otpauth://` URI）。

## 功能

- 可填写 `Issuer`、`Account`、`Secret(Base32)`。
- 固定输出 TOTP 参数：
  - `algorithm=SHA1`
  - `digits=6`
  - `period=30`
- 生成可被 Google Authenticator / Microsoft Authenticator 等应用扫码识别的二维码。

## 本地运行

```bash
python3 -m http.server 8000
```

打开 <http://localhost:8000> 即可。
