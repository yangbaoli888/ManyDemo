#!/usr/bin/env bash
set -euo pipefail

PORT="${PORT:-8080}"
DRY_RUN="false"

for arg in "$@"; do
  case "$arg" in
    --dry-run)
      DRY_RUN="true"
      ;;
    --port=*)
      PORT="${arg#*=}"
      ;;
    *)
      echo "未知参数: $arg"
      echo "用法: ./preview.sh [--dry-run] [--port=8080]"
      exit 1
      ;;
  esac
done

if ! command -v mvn >/dev/null 2>&1; then
  echo "未找到 mvn，请先安装 Maven。"
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "未找到 java，请先安装 JDK 17+。"
  exit 1
fi

echo "🚀 一键预览启动中..."
echo "访问地址: http://localhost:${PORT}"

CMD=(mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Dserver.port=${PORT}")

echo "执行命令: ${CMD[*]}"

if [ "$DRY_RUN" = "true" ]; then
  echo "dry-run 模式：仅打印命令，不实际启动。"
  exit 0
fi

exec "${CMD[@]}"
