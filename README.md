# NetLens

**网络分析 · 测速 · 优化建议**（中英双语 · Material You）

开源 Android 网络工具：覆盖 **WiFi Analyzer Pro** 核心能力，测速与诊断借鉴 **全球网测**，并加入多节点（含国内教育网/云镜像）与实用网络工具箱。

## 功能一览

### WiFi 分析
- 扫描：SSID / BSSID / 信号 / 2.4·5·6GHz / 信道 / 宽度 / 安全 / WiFi 4–7
- 当前连接：速率、IP、网关、DNS、厂商 OUI
- 信道图 + 评分 + 一键改信道引导
- 局域网设备扫描

### 测速（多节点）
- **国内**：阿里云 / 清华 TUNA / 中科大 USTC / 华为云 / 腾讯云 / 网易 / 上海交大
- **国际**：Cloudflare（含上传）/ Google DNS
- **本地网关**：仅延迟/丢包
- 双单位：**Mbps + MB/s**
- 节点详情：ISP、区域、Host、协议、样本大小

### 工具箱
- LAN 扫描、Ping、DNS 解析、HTTP 可用性、公网 IP

### 优化建议（可执行）
- 切换 5/6GHz（WifiNetworkSuggestion）
- 打开路由器后台 / 复制改信道步骤
- 一键复测

### 界面
- 底栏 Tab 直接切换（无返回栈）
- **莫奈取色**（Android 12+ Material You，可关）
- 深浅色主题、中英双语、无广告无追踪

## 商店上架前检查清单
- [ ] 隐私政策 URL（设置页已有文案，需填正式链接）
- [ ] 应用截图（6.7" 手机，中文+英文）
- [ ] 签名 keystore 与 Release 构建
- [ ] 分级问卷（工具类，通常 3+）
- [ ] 数据安全表单：不收集个人数据
- [ ] 版本说明写清权限用途（位置=扫描 WiFi）

## 构建
```bash
gradle assembleDebug
# 或 GitHub Actions 自动出包
```

## License
MIT


## 功能

### WiFi 分析（对标 WiFi Analyzer Pro）
- 扫描周边 WiFi：SSID / BSSID / 信号强度 / 频段（2.4 / 5 / 6 GHz）/ 信道 / 信道宽度
- 安全类型：OPEN / WEP / WPA / WPA2 / WPA3 / Enterprise
- WiFi 标准：WiFi 4 / 5 / 6 / 6E / 7
- 当前连接详情：链路速率、IP、网关、DNS、厂商 OUI
- 信道图与信道评分，推荐更空闲信道
- 局域网设备扫描（ARP + 并发探测）

### 网络测速（借鉴全球网测）
- 下载 / 上传吞吐（默认 Cloudflare 公共测速点）
- 延迟（ICMP ping，失败时 TCP 兜底）
- 抖动、丢包率
- 网络评分与等级（A+ ~ F）

### 智能优化建议
综合信号、信道拥挤度、安全协议、链路速率与测速结果，输出中英文建议：
- 弱信号 / 覆盖边缘
- 信道拥挤与切换推荐
- 过时或开放加密
- 协商速率与实测吞吐差距
- 高延迟 / 高抖动 / 高丢包

## 与 WiFi Analyzer Pro 功能对照

| WiFi Analyzer Pro | NetLens |
|---|---|
| 周边 WiFi 扫描与图表 | WiFi 列表 + 信号条 + 频段过滤 |
| 信道推荐 / Channel rating | 信道图 + 评分 + 最佳信道 |
| 当前网络详情（SSID/BSSID/厂商/速率/安全/DHCP） | 当前连接卡片 |
| 已连接设备 | 局域网设备扫描 |
| 信号与延迟分析 | RSSI 仪表 + Ping 延迟/抖动/丢包 |
| 导出结果 | 分享报告 |
| 深色/浅色主题、刷新间隔 | 设置页 |
| （Pro 相关）无广告开源替代 | MIT 开源、无广告无追踪 |

在以上基础上增加：完整吞吐测速（类似全球网测）与规则化优化建议。

## 技术栈
- Kotlin + Jetpack Compose + Material 3
- 单 Activity + Navigation Compose
- OkHttp 测速引擎
- 无广告、无追踪 SDK


## 构建

### 本机构建
需要 JDK 17 + Android SDK（compileSdk 35）：

```bash
gradle assembleDebug
# 或
./gradlew assembleDebug
```

APK 输出：`app/build/outputs/apk/debug/`

### GitHub Actions
推送到 GitHub 后，Actions 会自动构建 Debug / Release APK，可在 **Actions → Artifacts** 下载。

```bash
git init
git add .
git commit -m "feat: NetLens initial release"
git branch -M main
git remote add origin https://github.com/<you>/NetLens.git
git push -u origin main
```

## 权限说明
| 权限 | 用途 |
|------|------|
| ACCESS_WIFI_STATE / CHANGE_WIFI_STATE | WiFi 扫描与连接信息 |
| ACCESS_NETWORK_STATE | 网络状态 |
| ACCESS_FINE_LOCATION / COARSE_LOCATION | Android 扫描周边 WiFi 的系统要求 |
| NEARBY_WIFI_DEVICES | Android 13+ WiFi 设备 |
| INTERNET | 测速与连通性检测 |

## 目录结构
```
app/src/main/java/com/opensource/netlens/
  data/model/       数据模型
  data/wifi/        WiFi 扫描与连接
  data/speed/       测速 / Ping
  data/devices/     局域网设备
  data/advisor/     优化建议规则引擎
  ui/               Compose 界面
```

## License
MIT
