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


### Redis 系统命令

[命令查询](https://redis.io/commands)

#### 1、适合全体类型的常用命令

+ 判断 key 是否存在（存在返回1，否则返回0）：exists keyname
+ 删除 key（成功返回1，否则返回0）：del keyname
+ 返回 key 对应 value 的数据类型（none/string/list/set/zset（有序集合）/hash）：type keyname
+ 返回匹配的 key 列表：keys key-pattern
+ 随机获取一个已存在的 key：randomkey
+ 清空界面：clear
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

[Redis 实战读书笔记](https://github.com/daiDai-study/Learning-Redis/blob/master/%E8%AF%BB%E4%B9%A6%E7%AC%94%E8%AE%B0/Redis%E5%AE%9E%E6%88%98/Redis%E5%AE%9E%E6%88%98-%E8%AF%BB%E4%B9%A6%E7%AC%94%E8%AE%B0.md)
