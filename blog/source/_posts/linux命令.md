---
title: linux命令
date: 2024-03-11 18:17:22
lang: zh-cn
tags: linux
---

# linux命令

# awk

耗时日志截取  
 grep synthesize_voice demo.log | grep "200 OK"| awk -F "(" '{print $2}'| cut -d ',' -f 1

// 耗时平均值  
 grep synthesize_voice demo.log | grep "200 OK"|awk -F "(" '{gsub(/ms/,"",$2); print$2}' | cut -d ',' -f 1 | awk '{sum+=$1} END {print "average=", sum/NR}'

 awk '/200 OK.*synthesize_voice/{sum+=substr($(NF-2),2,index($(NF-2),"ms")-2)} END {print "average=", sum/NR}' demo.log

// 耗时平均值  
 awk '/200 OK.*synthesize_voice/{sum+=substr($(NF-2),2,index($(NF-2),"ms")-2);count++} END {print "average=", sum/count}' demo.log

```bash
#!/bin/bash

if [ -z "$1" ]; then echo "参数未传递，请重新输入" exit 1 fi
logPath=$1
IFS=$'\n'
declare -A map=()
log200=($(awk '/200 OK.*/{print $(NF-3), substr($(NF-2),2,index($(NF-2),"ms")-2)}'  ${logPath}))
for i in ${log200[@]};
do
    IFS=" "
	arr=($i)
	sum=0
	max=0
	min=0
	cun=0
	valueArr=(${map[${arr[0]}]})
	if [ ${#valueArr[@]} -eq 0 ]; then
	    valueArr=(0 0 ${arr[1]} 0)
	fi
	valueArr[0]=$((valueArr[0]+arr[1]))
	if [ ${valueArr[1]} -lt ${arr[1]} ]; then
	    valueArr[1]=${arr[1]}
	fi
	if [ ${valueArr[2]} -gt ${arr[1]} ]; then
	    valueArr[2]=${arr[1]}
	fi
	valueArr[3]=$[valueArr[3]+1]
	map[${arr[0]}]=${valueArr[@]}
done;

for key in "${!map[@]}"; then
do
	totalValue=(${map[$key]})
	echo 接口："<span data-type="inline-math" data-subtype="math" data-content="key"    请求数:" contenteditable="false" class="render-node"></span>{totalValue[3]},平均耗时:$[${totalValue[0]}/${totalValue[3]}],最大耗时:${totalValue[1]},最小耗时:${totalValue[2]}
done;
```

‍
