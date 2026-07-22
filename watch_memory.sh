#!/bin/bash
# =============================================================================
# watch_memory.sh   内存/磁盘 监控守护脚本（被动触发,可单跑也可Cron每5分钟调）
# 作用: 检查内存使用率 / 磁盘使用率 / Swap / 各进程内存TOP
#       若超过阈值 → 自动调用 clean_memory.sh 对应档(递进式 light→normal→deep)
#       所有动作写入 /var/log/watch_memory.log  方便复盘
# 使用:
#       bash watch_memory.sh                 # 单次检查(推荐放Cron)
#       bash watch_memory.sh --daemon 10     # 守护模式,每10秒循环(调试用)
# 建议放crontab:   */5 * * * * /opt/scripts/watch_memory.sh >>/var/log/watch_memory.log 2>&1
# =============================================================================
set +e
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CLEAN_SH="$SCRIPT_DIR/clean_memory.sh"
WATCH_LOG="/var/log/watch_memory.log"
STATE_FILE="/tmp/.watch_memory.state"

# ---- 阈值(可根据服务器改) ----
MEM_LIGHT=70
MEM_NORMAL=80
MEM_DEEP=88
SWAP_LIMIT=60
DISK_LIMIT=85
DEEP_COOLDOWN=1800

mem_pct() { free | awk '/Mem:/{printf("%.0f",$3/$2*100)}'; }
swap_pct(){ local s;s=$(free|awk '/Swap:/{if($2>0) printf("%.0f",$3/$2*100);else print 0}'); echo "${s:-0}"; }
disk_pct(){ df -P "${1:-/}" | awk 'NR==2{gsub(/%/,"",$5);print $5}'; }

mkdir -p "$(dirname "$WATCH_LOG")"
[ ! -f "$WATCH_LOG" ] && touch "$WATCH_LOG"

log() { local d;d=$(date '+%Y-%m-%d %H:%M:%S'); echo "[$d] $*" | tee -a "$WATCH_LOG"; }

last_deep_ago() {
  [ -f "$STATE_FILE" ] || echo 999999
  local last now delta
  last=$(awk -F'|' '/^DEEP/{print $2}' "$STATE_FILE" 2>/dev/null | tail -1)
  [ -z "$last" ] && { echo 999999; return; }
  now=$(date +%s)
  delta=$(( now - last ))
  [ "$delta" -lt 0 ] && delta=0
  echo "$delta"
}
mark_deep() {
  echo "DEEP|$(date +%s)|$1" >> "$STATE_FILE"
  tail -n 100 "$STATE_FILE" > "${STATE_FILE}.tmp" && mv "${STATE_FILE}.tmp" "$STATE_FILE"
}

top5_mem_proc() {
  ps -eo pid,ppid,pmem,rss,vsz,comm --sort=-rss 2>/dev/null | head -n 6
}

run_clean() {
  local mode="$1" reason="$2"
  log "★★★ 触发 $mode 清理 原因: $reason ★★★"
  [ -f "$CLEAN_SH" ] || { log "ERROR:找不到清理脚本 $CLEAN_SH,跳过"; return 1; }
  nohup bash "$CLEAN_SH" "$mode" >> "$WATCH_LOG" 2>&1
}

check_once() {
  local m s d top_mem_info lastd
  m=$(mem_pct); s=$(swap_pct); d=$(disk_pct)
  log "检测 MEM=$m%  SWAP=$s%  DISK(/)=${d}%"

  if [ "$m" -ge 60 ] || [ "$s" -ge 40 ]; then
    top_mem_info=$(top5_mem_proc | awk '{printf "    %s\n",$0}')
    log "内存TOP5进程:\n$top_mem_info"
  fi

  local triggered="no"

  if [ "$d" -ge "$DISK_LIMIT" ]; then
    lastd=$(last_deep_ago)
    if [ "$lastd" -ge "$DEEP_COOLDOWN" ]; then
      run_clean deep "DISK=${d}% >= ${DISK_LIMIT}%"
      mark_deep "DISK_$d"
      triggered="yes"
    else
      log "磁盘已高但deep冷却中($lastd s前刚做过,限制$DEEP_COOLDOWN s),只跑normal"
      run_clean normal "DISK=$d% high+deep冷却"
      triggered="yes"
    fi
  fi

  if [ "$triggered" = "no" ] && [ "$m" -ge "$MEM_DEEP" ]; then
    lastd=$(last_deep_ago)
    if [ "$lastd" -ge "$DEEP_COOLDOWN" ]; then
      run_clean deep "MEM=${m}% >= ${MEM_DEEP}%"
      mark_deep "MEM_$m"
    else
      log "内存已超DEEP线$m%,但deep冷却中($lastd s前刚做过),降级normal"
      run_clean normal "MEM=$m% high,deep cooldown"
    fi
    triggered="yes"
  fi

  if [ "$triggered" = "no" ] && [ "$s" -ge "$SWAP_LIMIT" ] && [ "$m" -le 90 ]; then
    run_clean normal "SWAP=${s}% >= ${SWAP_LIMIT}% (MEM=$m% ok)"
    triggered="yes"
  fi

  if [ "$triggered" = "no" ] && [ "$m" -ge "$MEM_NORMAL" ]; then
    run_clean normal "MEM=${m}% >= ${MEM_NORMAL}%"
    triggered="yes"
  fi

  if [ "$triggered" = "no" ] && [ "$m" -ge "$MEM_LIGHT" ]; then
    run_clean light "MEM=${m}% >= ${MEM_LIGHT}%"
    triggered="yes"
  fi

  [ "$triggered" = "no" ] && log "状态OK,无需清理."
  return 0
}

if [ "$1" = "--daemon" ]; then
  INTERVAL="${2:-30}"
  log "【启动守护模式】检查间隔=${INTERVAL}秒  CLEAN_SH=$CLEAN_SH"
  while true; do
    check_once
    sleep "$INTERVAL"
  done
else
  check_once
fi
exit 0
