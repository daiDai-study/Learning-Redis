# Redis 学习笔记

## Redis 推荐学习资源

[Redis 简明教程-实验楼](https://www.shiyanlou.com/courses/106)
书籍《Redis 实战》
[Redis 官方网站](https://redis.io)


## Redis 基础学习

通过学习[Redis 简明教程-实验楼](https://www.shiyanlou.com/courses/106)对Redis有基础了解

### 基础了解

> Redis（REmote DIctionary Server）是一个 key-value 存储系统，提供了丰富的数据结构及相关操作。
> Redis 是一个服务端和客户端配合的程序。
> Reids 中，命令大小写不敏感。

Redis 的优点：
+ 性能高：能支持超过 100K+ 每秒的读写频率
+ 丰富的数据类型：支持（**二进制安全的**）Strings、Lists、Sets、OrderedSets 和 Hashes
+ 原子性：Redis 的所有操作都是原子性的，同时 Redis 还支持对几个操作合并后的原子性执行
+ 丰富的特性：支持 publish/subscribe、通知、key过期等特性

### 基本数据类型及相关操作（命令）

[命令查询](https://redis.io/commands)

#### 1、字符串-string
+ 单个设置（keyname 不存在时为创建，存在时为替换）：set keyname val
+ 单个设置强制为创建（keyname 不存在时才成功）：set keyname val nx
+ 单个设置强制为替换（keyname 存在时才成功）：set keyname val xx
+ 单个获取：get keyname
+ 加减1（只有当 keyname 对应的 value 是 number 类型的字符串）：incr/decr keyname
+ 加减（只有当 keyname 对应的 value 是 number 类型的字符串）：incrby/decrby keyname number
+ 设置多个：mset keyname1 val [keyname2 val]
+ 获取多个：mget keyname1 [keyname2]

#### 2、列表-list

+ 头部插入：lpush keyname val1 [val2] 
+ 尾部插入：rpush keyname val1 [val2] 
+ 获取指定范围（0 表示 list 开头第一个，-1 表示 list 的倒数第一个）：lrange keyname 0 -1
+ 头部删除：lpop keyname
+ 尾部删除：rpop keyname

#### 3、无序集合-set

+ 添加：sadd keyname val1 [val2]
+ 查看集合：smembers keyname
+ 查看集合元素是否存在：sismember keyname val

#### 4、散列-hash

+ 设置一个多字段的 hash 表：hmset keyname field1 val1 [field2 val2]
+ 获取指定字段：hget keyname filedname
+ 获取所有字段：hgetall keyname
+ 获取多个字段：hmget keyname filed1 [filed2]
+ 数字加操作：hincrby keyname filedname number

#### 5、有序集合-zset（orderedset）

+ 添加：zadd keyname score val
+ 查看指定范围正序的集合（withscores 参数返回 scores）：zrange keyname 0 -1
+ 查看指定范围反序的集合（withscores 参数返回 scores）：zrevrange keyname 0 -1



### Redis 系统命令

#### 1、适合全体类型的常用命令

+ 判断 key 是否存在（存在返回1，否则返回0）：
+ 删除 key（成功返回1，否则返回0）：
+ 返回 key 对应 value 的数据类型（none/string/list/set/zset（有序集合）/hash）：type keyname
+ 返回匹配的 key 列表：keys key-pattern
+ 随机获取一个已存在的 key：randomkey
+ 清楚界面：clear
+ 更改 key 的名字（如果新键存在将被覆盖）：rename oldname newname
+ 更改 key 的名字（如果新键存在更新失败）：renamenx oldname newname
+ 返回当前数据库中 key 的总数：dbsize
  
#### 2、Redis 时间相关命令

+ 设置指定 key 的过期时间（秒）（在没有过期的时候，对值进行了改变，过期限制将被清除）：expire keyname number（set keyname val ex number）
+ 查看 key 剩余生存时间（秒）：ttl keyname
+ 清空当前数据库中的所有键：flushdb
+ 清空所有数据库中的所有键：flushall

#### 3、Redis 设置相关命令

+ 读取运行 Redis 服务器的配置参数：config get configkey
+ 更改运行 Redis 服务器的配置参数：config set configkey configvalue
+ 认证密码：auth requirepass-value（密码）
+ 重置数据统计报告：config resetstat

#### 4、查询信息

> Redis 配置信息存放路径：
> + Linux：/etc/redis/redis.conf

+ 返回所有信息：info
+ 返回指定信息：info [section]

### Redis 高级特性

> 高级特性包括：主从复制、事务处理、持久化机制、虚拟内存的使用（2.6 及之上版本取消）


### 个人理解

Redis 中 Value 的数据类型包含多种，如 String、List、Hash、Set、SortedSet，其中 Hash 数据结构可以在对 Java 中 HashMap 有深入了解之后，再回来深入了解 Redis 中 Hash 数据类型

## Redis 深入学习

通读书籍《Redis 实战》