.PHONY: clean package help

# Maven 命令
MVN = mvn

# 默认目标
.DEFAULT_GOAL := help

# 清理构建产物
clean:
	$(MVN) clean

# 打包项目（跳过测试）
package:
	$(MVN) clean package -DskipTests

# 显示帮助信息
help:
	@echo "可用的 Make 目标:"
	@echo "  clean    - 清理构建产物 (删除 target 目录)"
	@echo "  package  - 清理并打包项目 (生成 JAR 文件)"

