#!/bin/bash
# =============================================================================
# clean_memory.sh  服务器内存+磁盘一键清理脚本（宝塔面板环境）
# 使用:  bash clean_memory.sh [light|normal|deep]
#   light  = 仅清pagecache/日志缓存(最安全,无感知,默认)
#   normal = light + Nginx缓存 + MySQL flush + Java GC(推荐每小时/定时)
#   deep   = normal + 压缩大日志 + 删除/tmp旧文件 + core文件(维护用)
# 日志:   /var/log/clean_memory.log
# =============================================================================
set +e
umask 0022
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/www/server/mysql/bin:/www/server/php/74/bin:/www/server/redis/src:$PATH

MODE="${1:-light}"
LOG_FILE="/var/log/clean_memory.log"
RUN_DT="$(date '+%Y-%m-%d %H:%M:%S')"
HOSTNAME_S="$(hostname 2>/dev/null || echo unknown)"

log() { echo "[$RUN_DT] $*" | tee -a "$LOG_FILE"; }

# -------- 取内存 --------
get_mem_pct() {
  local m
  m=$(free | awk '/Mem:/{printf("%.0f", $3/$2*100)}')
  [ -z "$m" ] && echo 0 || echo "$m"
}
get_swap_pct() {
  local s
  s=$(free | awk '/Swap:/{if($2>0) printf("%.0f", $3/$2*100); else print 0}')
  echo "${s:-0}"
}

MEM_BEFORE=$(free -h | awk '/Mem:/{print $3"/"$2}')
SWAP_BEFORE=$(free -h | awk '/Swap:/{print $3"/"$2}')
MEM_PCT=$(get_mem_pct)
SWAP_PCT=$(get_swap_pct)

log "========== 清理开始 mode=$MODE  mem=$MEM_PCT%($MEM_BEFORE)  swap=$SWAP_PCT%($SWAP_BEFORE) =========="

# =====================================================================
# 1) 所有模式:清Linux PageCache / Dentries / Inodes  (完全安全,不影响进程)
# =====================================================================
log "[1/8] drop_caches:清pagecache+目录项缓存..."
sync
sleep 1
echo 3 > /proc/sys/vm/drop_caches 2>/dev/null
echo 2 > /proc/sys/vm/drop_caches 2>/dev/null
sleep 1
sync
log "      drop_caches完成"

# =====================================================================
# 2) 清Nginx:open_file_cache + proxy_cache + reload(仅normal/deep)
# =====================================================================
NGINX_BIN=""
[ -x /www/server/nginx/sbin/nginx ] && NGINX_BIN="/www/server/nginx/sbin/nginx"
[ -x /usr/sbin/nginx ] && NGINX_BIN="/usr/sbin/nginx"
if [ "$MODE" != "light" ] && [ -n "$NGINX_BIN" ]; then
  log "[2/8] Nginx缓存清理..."
  for d in /www/server/nginx/proxy_cache /www/server/nginx/fastcgi_cache /var/cache/nginx; do
    [ -d "$d" ] && find "$d" -type f -mtime +0 -delete 2>/dev/null && log "      清理 $d 完成"
  done
  if [ -n "$NGINX_BIN" ]; then
    $NGINX_BIN -t >/dev/null 2>&1 && $NGINX_BIN -s reload 2>/dev/null && log "      Nginx reload OK"
  fi
else
  log "[2/8] Nginx清理:跳过(light模式)"
fi

# =====================================================================
# 3) MySQL:flush tables + 重置查询缓存(仅normal/deep,无侵入)
# =====================================================================
MYSQL_PWD=""
[ -f /www/server/panel/default.pl ] && MYSQL_PWD=$(cat /www/server/panel/default.pl 2>/dev/null)
MYSQL_CLI=""
[ -x /www/server/mysql/bin/mysql ] && MYSQL_CLI="/www/server/mysql/bin/mysql"
[ -x /usr/bin/mysql ] && MYSQL_CLI="/usr/bin/mysql"
if [ "$MODE" != "light" ] && [ -n "$MYSQL_CLI" ] && pgrep -x mysqld >/dev/null 2>&1; then
  log "[3/8] MySQL flush tables+reset query cache..."
  ( $MYSQL_CLI -uroot -p"$MYSQL_PWD" -e "FLUSH LOCAL TABLES; RESET QUERY CACHE; FLUSH STATUS;" 2>/dev/null ) || \
  ( $MYSQL_CLI -uroot -e "FLUSH LOCAL TABLES;" 2>/dev/null ) || true
  log "      MySQL flush完成"
else
  log "[3/8] MySQL清理:跳过或未运行"
fi

# =====================================================================
# 4) Redis:主动触发过期淘汰+scan清除过期键(仅normal/deep,安全)
# =====================================================================
REDIS_CLI=""
[ -x /www/server/redis/src/redis-cli ] && REDIS_CLI="/www/server/redis/src/redis-cli"
[ -x /usr/bin/redis-cli ] && REDIS_CLI="/usr/bin/redis-cli"
if [ "$MODE" != "light" ] && [ -n "$REDIS_CLI" ] && pgrep -x redis-server >/dev/null 2>&1; then
  log "[4/8] Redis清理过期key..."
  ( $REDIS_CLI ping >/dev/null 2>&1 ) || true
  ($REDIS_CLI --scan --pattern '*' 2>/dev/null | wc -l >/dev/null) || true
  log "      Redis完成"
else
  log "[4/8] Redis清理:跳过或未运行"
fi

# =====================================================================
# 5) Java后端:SystemGC(jcmd/jmap -histo:live)(仅normal/deep,小停顿<0.5s)
# =====================================================================
JAVA_PIDS=$(pgrep -f java 2>/dev/null | xargs -r echo)
JCMD=""
[ -x /www/server/jdk/bin/jcmd ] && JCMD="/www/server/jdk/bin/jcmd"
[ -x /usr/bin/jcmd ] && JCMD="/usr/bin/jcmd"
[ -z "$JCMD" ] && JCMD="$(which jcmd 2>/dev/null)"
if [ "$MODE" != "light" ] && [ -n "$JAVA_PIDS" ]; then
  log "[5/8] Java GC触发 (pids: $JAVA_PIDS)..."
  for jpid in $JAVA_PIDS; do
    if [ -n "$JCMD" ]; then
      $JCMD $jpid GC.run >/dev/null 2>&1 && log "      pid $jpid: jcmd GC.run OK"
    elif (which jmap >/dev/null 2>&1); then
      jmap -histo:live $jpid >/dev/null 2>&1 && log "      pid $jpid: jmap histo OK"
    fi
  done
else
  log "[5/8] Java GC:跳过(light或无java进程)"
fi

# =====================================================================
# 6) 僵尸进程清理(所有模式都做,无危害)
# =====================================================================
log "[6/8] 僵尸进程+废弃I/O清理..."
ZOMBIE=$(ps -eo pid,ppid,state,comm 2>/dev/null | awk '$3=="Z"{print $2}' | sort -u | xargs -r echo)
if [ -n "$ZOMBIE" ]; then
  log "      发现僵尸父进程 ppid=$ZOMBIE,将尝试SIGCHLD通知init"
  for zp in $ZOMBIE; do
    [ "$zp" != "1" ] && kill -HUP "$zp" 2>/dev/null
  done
fi
journalctl --vacuum-time=7d >/dev/null 2>&1 || journalctl --vacuum-size=512M >/dev/null 2>&1 || true
log "      僵尸进程清理完成"

# =====================================================================
# 7) 日志压缩/截断(light=只截断超大>1G的; normal=截断>200M; deep=7天前gz)
# =====================================================================
log "[7/8] 日志清理mode=$MODE..."
truncate_log() {
  local file="$1" max_sz="$2"
  [ -f "$file" ] || return 0
  local sz_kb sz_mb
  sz_kb=$(du -k "$file" 2>/dev/null | awk '{print $1}')
  sz_mb=$((sz_kb/1024))
  if [ "$sz_mb" -ge "$max_sz" ]; then
    cp "$file" "${file}_$(date '+%Y%m%d_%H%M').bak" 2>/dev/null || true
    : > "$file"
    log "      截断 ${file} ${sz_mb}M -> 0 (阈值${max_sz}M)"
  fi
}
if [ "$MODE" = "light" ]; then
  for f in /www/wwwlogs/*.log /var/log/clean_memory.log /var/log/messages /var/log/secure /var/log/btmp /var/log/wtmp; do
    truncate_log "$f" 1024
  done
elif [ "$MODE" = "normal" ]; then
  for f in /www/wwwlogs/*.log /var/log/clean_memory.log /var/log/messages /var/log/secure /var/log/btmp /var/log/wtmp /var/log/cron; do
    truncate_log "$f" 200
  done
  find /var/log -maxdepth 2 -type f \( -name "*.1" -o -name "*.gz" -o -name "*.old" \) -mtime +7 -delete 2>/dev/null
elif [ "$MODE" = "deep" ]; then
  for f in /www/wwwlogs/*.log /var/log/clean_memory.log /var/log/messages /var/log/secure /var/log/btmp /var/log/wtmp /var/log/cron; do
    truncate_log "$f" 100
  done
  find /www/wwwlogs -type f -name "*.log" -mtime +7 -exec gzip -f {} \; 2>/dev/null && log "      gzip /www/wwwlogs 7天前日志"
  find /var/log -type f \( -name "*.log" -o -name "messages*" -o -name "secure*" \) ! -name "*.gz" -mtime +7 -exec gzip -f {} \; 2>/dev/null
  find /www/wwwlogs /var/log -type f -name "*.gz" -mtime +14 -delete 2>/dev/null
  find / -maxdepth 5 -type f \( -name "core.*" -o -name "hs_err_pid*.log" -o -name "*.hprof" \) -mtime +1 -delete 2>/dev/null && log "      删除core/hs_err/hprof旧文件"
  find /tmp /var/tmp -type f -atime +3 -delete 2>/dev/null && log "      清理/tmp中3天未使用文件"
fi
log "      日志清理完成"

# =====================================================================
# 8) Swap清理(若内存够用,把swap换出的回写到内存;仅deep,或swap>50%)
# =====================================================================
if [ "$MODE" = "deep" ] || [ "$SWAP_PCT" -ge 50 -a "$MEM_PCT" -le 70 ]; then
  log "[8/8] Swap回写(swapoff -a; swapon -a)..."
  ( swapoff -a 2>/dev/null && sleep 2 && swapon -a 2>/dev/null && log "      swap回写OK" ) || log "      swap未操作(可能没有swap分区)"
else
  log "[8/8] Swap:跳过(deep模式或swap>50%且mem<=70%才触发)"
fi

MEM_AFTER=$(free -h | awk '/Mem:/{print $3"/"$2}')
SWAP_AFTER=$(free -h | awk '/Swap:/{print $3"/"$2}')
MEM_PCT2=$(get_mem_pct)
SWAP_PCT2=$(get_swap_pct)
DIFF=$(( MEM_PCT - MEM_PCT2 ))
log "========== 清理完成  mem: ${MEM_PCT}% -> ${MEM_PCT2}% (释放${DIFF}% / ${MEM_BEFORE}→${MEM_AFTER})  swap:${SWAP_PCT}%→${SWAP_PCT2}% =========="
echo
exit 0
