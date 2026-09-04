<div align="center">

# Deterministic Chance · 确定的概率

**把机器里的概率，写成 AE2 的确定答案。**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B47A?style=for-the-badge)](#运行环境)
[![Forge](https://img.shields.io/badge/Forge-47.4.3-E04E14?style=for-the-badge)](#运行环境)
[![Build](https://img.shields.io/github/actions/workflow/status/LangQi99/DeterministicChance/build.yml?branch=main&style=for-the-badge&label=Build)](https://github.com/LangQi99/DeterministicChance/actions/workflows/build.yml)
[![License](https://img.shields.io/github/license/LangQi99/DeterministicChance?style=for-the-badge)](LICENSE)

<img src="docs/images/mekanism-chance-recipe.png" alt="Mekanism 概率机器配方" width="900">

<br>

<img src="docs/images/ae2-deterministic-pattern.png" alt="AE2 确定概率处理样板" width="640">

<br>

<sub>“我破解了概率样板。”</sub>

</div>

---

## 这是什么？

**Deterministic Chance（确定的概率）** 是一个连接
[Mekanism](https://github.com/mekanism/Mekanism)、
[Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2)
与 [JEI](https://github.com/mezz/JustEnoughItems) 的 Forge 模组。

它把机器的概率产出换成严格可预测的循环，并在从 JEI 向 AE2 样板编码终端填充配方时，自动生成结果完全匹配的批量处理样板。

> 如果原配方是 `1 A → 80% × 1 B`，机器会固定执行“出、出、出、出、不出”，AE2 则直接编码为 `5 A → 4 B`。

## 从概率配方到确定样板

以 Mekanism 精密锯木机的橡木配方为例：

```text
原配方：1 原木 → 6 木板 + 25% × 1 锯末
AE 样板：4 原木 → 24 木板 + 1 锯末
机器序列：出锯末、不出、不出、不出，然后循环
```

JEI 仍然忠实展示原模组定义的单次概率；点击配方填充按钮时，Deterministic Chance 会把它换算成最小完整周期后再写入 AE2。

## 核心功能

| 功能 | 说明 |
| --- | --- |
| 确定序列 | 用固定循环替代机器的独立随机判定，每个周期严格命中指定次数 |
| 精确样板 | 自动把 `80%` 编成 `5 → 4`、把 `25%` 编成 `4 → 1` |
| JEI 联动 | 在 AE2 样板编码终端点击 JEI 填充时自动完成批次换算 |
| 主产物与副产物 | 固定主产物按完整周期放大，概率副产物按命中次数写入 |
| 多输入、多输出 | 通用计划器支持多个输入、固定输出和多个独立概率输出 |
| 最小批次 | 对多个概率分母求最小公倍数，生成最小的整数输入输出计划 |
| 原机逻辑保留 | 不改机器原有耗能、耗时、升级与输入输出流程，只接管概率结果 |

## 当前支持

目前已经完成第一套可玩的实际适配：

- **Mekanism 精密锯木机**：实际副产物由确定序列控制。
- **AE2 + JEI**：所有 Mekanism 锯切配方在填入样板编码终端时自动换算完整周期。
- **通用概率核心**：支持约分、序列推进、多输入、多输出、副产物与最小精确批次计算。

当前 Mekanism 原型按已加载的配方对象保存序列相位，适合单机和单台机器验证。后续会把状态升级为
`维度 + 机器位置 + 配方 ID + 输出槽` 的服务端存档数据，覆盖多台机器并在重启后继续原来的循环。

## 为什么仍需要机器适配？

Forge、AE2 和 JEI 没有统一的“概率输出”协议。JEI 负责展示配方，但不决定机器在什么时候、以什么方式掷概率；不同机器模组也可能把随机判定放在完全不同的执行流程中。

因此本项目采用 **通用核心 + 模组级适配器**：

- 通用核心负责把概率变成精确分数、计算最小批次并推进确定序列。
- 模组适配器负责读取原生概率，并在机器真正提交产物的位置接管结果。
- 同一模组如果共享中央概率实现，一个适配器通常就能覆盖一整类机器，无需逐台修改。

更完整的设计与兼容边界见 [架构说明](docs/ARCHITECTURE.md)。

## 运行环境

| 依赖 | 版本 |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.4.3 或兼容的 47.x 版本 |
| Applied Energistics 2 | 15.4.10 |
| Mekanism | 10.4.16 |
| JEI | 15.49.x |
| Java | 17 |

## 开发构建

```bash
./gradlew build
./gradlew runClient
```

构建完成的模组位于 `build/libs/`。项目通过公开依赖引用 AE2、Mekanism 与 JEI，不会将它们的源码或资源打包进本模组。

## 许可证

Deterministic Chance 使用 [MIT License](LICENSE) 开源。AE2、Mekanism 与 JEI 的内容仍分别受其自身许可证约束。
