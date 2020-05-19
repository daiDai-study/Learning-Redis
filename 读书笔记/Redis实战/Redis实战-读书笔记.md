# Redis实战-读书笔记

## 第零部分 相关资源
[书中实例代码](https://github.com/josiahcarlson/redis-in-action)
[Spring使用Redis](https://spring.io/projects/spring-data-redis)

## 第一部分 入门

## 第1章 初识Redis

### 1.1 Redis 简介 
> Redis 是一个使用内存存储的非关系数据库（non-relational database，NoSQL），可以称之为内存数据库（*准确的说，是内存数据库服务器*），可以存储键和各种不同类型（五种基本数据结构类型：string/list/hash/set/zset）的值之间的映射（所以又称为数据结构服务器），可以将存储在内存中的键值对数据持久化到硬盘中（持久化机制），可以使用复制特性来扩展读性能（*主从复制*），还可以使用客户端分片（*什么是客户端分片？*）来扩展写性能。

**Memcached（键值缓存服务器）与Redis**
> Memcached 是使用内存存储的键值缓存
+ 持久化：Redis可以持久化到硬盘，Memcached不可以
+ 数据结构：Redis存储的键值对中的值包含多种数据结构类型，Memcached只能存储字符串类型的值

**搞清楚Redis是什么、能做什么以及为什么要使用它？**

+ 能做什么（具有哪些特性：久化、主从复制）：可以作为缓存，可以作为数据库
+ 为什么使用：简单（易用、易维护）、快速（执行速度快）

### 1.2 Redis 基本数据结构

结构类型|结构存储的值|结构的读写能力
:---|:--:|:---
STRING|字符串，但可以将整数或浮点数的字符串识别为整数或浮点数|对整个字符串（`set/get/mset/mget`）或字符串的其中一部分执行操作；对整数或浮点数执行自增或自减操作（`incr/incrby/decr/decrby`）
LIST|列表，列表（双向链表）上的每个节点都包含一个字符串（*隐含意思：不能为 number*）|从链表的两端（头部为left、尾部为right）推入（`lpush/rpush`）或弹出（`lpop/rpop`）元素；根据偏移量对链表进行修剪；读取单个（`lindex`）或者多个（`lrange`）元素；根据值查找或移除元素
SET|无序集合，没有重复元素|添加（`sadd`）、获取（`smembers`）、移除（`srem`）元素；检查一个元素是否存在于集合中（`sismember`）；计算交集、并集、差集；从集合中随机获取元素（`srandmember`）
HASH|包含键值对（field:value）的无序散列表|添加（`hset/hmset`）、获取（`hget/hgetall/hmget`）、移除（`hdel`）键值对
ZSET|字符串成员（member）与浮点数分值（score）之间的有序映射，元素的排列由分值的大小决定|添加（`zadd`）、获取（`zrange`）、移除（`zrem`）元素；根据分值范围（`zrangebyscore`）或者成员来获取元素；根据成员获取分值（`zscore`）



#### 1.2.1 Redis 中的字符串

![](https://redislabs.com/wp-content/uploads/2019/07/data-structures-_strings.svg)

#### 1.2.2 Redis 中的列表
注意：列表中，**相同元素可以重复出现**

![](https://redislabs.com/wp-content/uploads/2019/07/data-structures-_lists.svg)

#### 1.2.3 Redis 中的集合
> 集合是通过散列表来保证自己存储的每个字符串独一无二（这些散列表只有键，但没有与键相关联的值）

`smembers` 命令： 获取集合中所有元素，如果元素非常多，该命令的执行速度可能会很慢，该命令谨慎使用

![](https://redislabs.com/wp-content/uploads/2019/07/data-structures-_sets.svg)

#### 1.2.4 Redis 中的散列
> Redis的散列可以看作文档数据库中的文档，或者看作关系数据库中的行，因为散列、文档和行这三者都允许用户同时访问或者修改一个或者多个域（field，又称字段）

![](https://redislabs.com/wp-content/uploads/2019/07/data-structures-_hashes.svg)

#### 1.2.5 Redis 中的有序集合
> 有序集合和散列类似，都用于存储键值对：
> + 有序集合的键被称为成员（member），每个成员都是各不相同
> + 有序集合的值被称为分值（score），分值必须为浮点数
> 
> 有序集合是Redis中唯一一个既可以根据成员访问元素，又可以根据分值以及分值的排列顺序来访问元素的结构。

![](https://redislabs.com/wp-content/uploads/2019/07/data-structures-_sorted-sets.svg)


#### 1.2.6 个人理解

1. **Redis 中的集合、散列、有序集合都是通过散列表实现**
2. *list/set/hash/zset（哪些）中的元素（元素中的值）可以是数字（整数或浮点数）？*
   + hash 中存储的值可以是数字值


### 1.3 实战

#### 1.3.1 使用 Redis 来构建一个简单的文章投票网站的后端

## 第2章 使用 Redis 构建 Web 应用

## 第二部分 
## 第三部分 

