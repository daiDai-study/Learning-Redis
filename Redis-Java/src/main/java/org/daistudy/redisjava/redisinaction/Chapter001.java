package org.daistudy.redisjava.redisinaction;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ZParams;

import java.util.*;

/**
 * 需求：使用 Redis 构建一个简单的文章投票网站后端
 *      网站根据文章的发布时间和文章获得的投票数量计算出一个评分，并可以按照评分决定如何排序和展示文章
 *
 * 评分：可以随着时间流逝而不断减少（暂时先不做）
 *      文章的评分 = 文章得到的支持票数乘以一个常量【 + 文章的发布时间】评分计算方法有问题
 *      从Unix时间（UTC时区1970年1月1日）到现在为止经过的秒数来计算文章的评分
 *
 * 信息及其存储结构：
 *      单个文章信息 -- hash -- article:articleId 其中投票数量默认为1
 *      根据发布时间排序的文章列表 -- zset -- time:
 *      根据评分排序的文章列表 -- zset -- score:
 *      根据发布时间排序的群组的文章列表 -- zset -- time:groupId （该集合是 time: 和 group:groupId 的 zinterstore）
 *      根据评分排序的群组的文章列表 -- zset -- score:groupId （该集合是 score: 和 group:groupId 的 zinterstore）
 *      每篇文章的已投票用户名单（不能重复投票） -- set -- voted:articleId（需要设置过期时间，单纯给一个没有元素的键设置过期时间，好像是没有效果）
 *      用户集合 -- set -- user:list（集合元素的值为 user:userId）
 *      文章集合 -- set -- article:list（集合元素的值为 article:articleId）
 *      群组集合 -- set -- group:list（集合元素的值为 group:groupId）
 *      文章ID的计数器 -- string -- article:
 *      用户ID的计数器 -- string -- user:
 *      群组ID的计数器 -- string -- group:
 *      同属一个群组中的文章ID记录集合 -- set -- group:groupId
 *
 * 规定：
 *      当一篇文章发布期满一周之后，用户将不能再对其进行投票，文章的评分将被固定下来，而文章的已投票用户名单的集合也会被删除
 *
 */
public class Chapter001 {
    private static final int ONE_WEEK_IN_SECONDS = 7 * 86400;
    private static final int VOTE_SCORE = 10;
    private static final int ARTICLES_PER_PAGE = 25;

    public static void main(String[] args) {
        new Chapter001().run();
    }

    public void run() {
        Jedis conn = init();

        Random random = new Random();
        String userRandom;
        String articleRandom;

        // 发布 10-100 篇文章
        int count = random.nextInt(90) + 10;
        int i = 0;
        while(i < count){
            userRandom = conn.srandmember("user:list");
            postArticle(conn, userRandom, "A title", "http://www.google.com");
            i++;
        }

        i = 0;
        while(i < count){
            userRandom = conn.srandmember("user:list");
            articleRandom = conn.srandmember("article:list");
            articleVote(conn, userRandom, articleRandom);
            String votes = conn.hget(articleRandom, "votes");
            System.out.println("用户"+ userRandom +"给文章" + articleRandom + "投票了，现在票数是： " + votes);
            assert Integer.parseInt(votes) > 1;
            i++;
        }


        System.out.println("目前评分最高的文章列表:");
        List<Map<String,String>> articles = getArticles(conn, 1);
        printArticles(conn, articles);
        assert articles.size() >= 1;


//        addArticleToGroups(conn, articleId, new String[]{"new-group"});
//        System.out.println("We added the article to a new group, other articles include:");
//        articles = getGroupArticles(conn, "new-group", 1);
//        printArticles(articles);
//        assert articles.size() >= 1;
    }

    private void printArticles(Jedis conn, List<Map<String, String>> articles) {
        for (Map<String,String> article : articles){
            System.out.println("  id: " + article.get("id"));
            for (Map.Entry<String,String> entry : article.entrySet()){
                if (entry.getKey().equals("id")){
                    System.out.println("    评分: " + conn.zscore("score:", "article:" + article.get("id")));
                    continue;
                }
                System.out.println("    " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }

    /**
     * Jedis 的初始化
     */
    public Jedis init(){
        Jedis conn = new Jedis("localhost");
        conn.select(15);
        conn.flushDB();
        generateUsers(conn);
        generateGroups(conn);
        return conn;
    }

    /**
     * 创建用户集合
     * @param conn Jedis
     */
    private void generateUsers(Jedis conn){
        genereteSet(conn, "user:");
    }

    /**
     * 创建群组集合
     * @param conn Jedis
     */
    private void generateGroups(Jedis conn){
        genereteSet(conn, "group:");
    }

    /**
     * 创建集合
     * @param conn Jedis
     * @param key 集合的键
     */
    private void genereteSet(Jedis conn, String key){
        if(!conn.exists(key)){
            conn.set(key, "10000");
        }
        // 随机递增（1-101）生成100个值
        Random random = new Random();
        int i = 0;
        while(i < 100){
            int incr = random.nextInt(100) + 1;
            conn.incrBy(key, incr);
            conn.sadd(key + "list", key + conn.get(key));
            i++;
        }
    }

    /**
     * 文章投票
     * @param conn Redis
     * @param user 用户
     * @param article 文章
     */
    public void articleVote(Jedis conn, String user, String article){
        // 1. 判断文章发布期是否满一周
        long cutoff = (System.currentTimeMillis() / 1000) - ONE_WEEK_IN_SECONDS;
        if (conn.zscore("time:", article) < cutoff) {
            return;
        }

        // 2. 未满一周才进行投票
        String articleId = article.substring(article.indexOf(":") + 1);
        // 2.1 用户投票成功（不能重复投票）--文章已投票用户名单增加用户
        if (conn.sadd("voted:" + articleId, user) == 1) {
            // 2.2 文章评分递增
            conn.zincrby("score:", VOTE_SCORE, article);
            // 2.3 文章的投票数量加1
            conn.hincrBy(article, "votes", 1);
        }
    }

    /**
     * 发布文章
     * @param conn Jedis
     * @param user 发布人
     * @param title 发布文章的标题
     * @param link 发布文章的链接
     */
    private String postArticle(Jedis conn, String user, String title, String link){
        // 1.1 生成文章ID

        if (conn.exists("article:")) {
            conn.incrBy("article:", new Random().nextInt(100) + 1).toString();
        }else{
            conn.set("article:", "10000");
        }
        String articleId = conn.get("article:");
        String article = "article:" + articleId;

        // 1.2 将文章添加到文章集合中
        conn.sadd("article:list", article);

        // 2. 生成文章已投票用户名单
        String voted = "voted:" + articleId;
        conn.sadd(voted, user);
        conn.expire(voted, ONE_WEEK_IN_SECONDS);

        // 3. 设置文章信息
        long now = System.currentTimeMillis() / 1000;
        Map<String, String> articleData = new HashMap<>();
        articleData.put("id", articleId);
        articleData.put("title", title);
        articleData.put("link", link);
        articleData.put("user", user);
        articleData.put("now", String.valueOf(now));
        articleData.put("votes", "1");
        conn.hmset(article, articleData);

        // TODO 4. 设置文章评分（评分机制有问题）
        conn.zadd("score:", VOTE_SCORE, article);

        // 5. 按时间排序的文章
        conn.zadd("time:", now, article);

        return articleId;
    }

    /**
     * 获取文章
     * @param conn Jedis
     * @param page 第几页
     * @return 默认从评分集合中获取
     */
    public List<Map<String, String>> getArticles(Jedis conn, int page){
        return getArticles(conn, page, "score:");
    }

    /**
     * 获取文章
     * @param conn Jedis
     * @param page 第几页
     * @param order 排序方式 == 从哪个集合中获取文章
     * @return 文章列表
     */
    public List<Map<String, String>> getArticles(Jedis conn, int page, String order){
        // 1. 获取第 page 页的文章ID
        int start = (page - 1) * ARTICLES_PER_PAGE;
        int end = start + ARTICLES_PER_PAGE - 1;
        Set<String> articleIds = conn.zrevrange(order, start, end);

        // 2. 填充文章信息
        List<Map<String, String>> articles = new ArrayList<>();
        for (String articleId : articleIds) {
            Map<String, String> articleData = conn.hgetAll(articleId);
            articles.add(articleData);
        }
        return articles;
    }

    /**
     * 将文章添加到群组中
     * @param conn Jedis
     * @param article 文章
     * @param toAdd 群组
     */
    public void addArticleToGroups(Jedis conn, String article, String[] toAdd){
        for (String group : toAdd) {
            conn.sadd("group:" + group, article);
        }
    }

    public List<Map<String, String>> getGroupArticles(Jedis conn, String groupId, int page){
        return getGroupArticles(conn, groupId, page, "score:");
    }

    public List<Map<String, String>> getGroupArticles(Jedis conn, String groupId, int page, String order){
        String key = order + groupId;
        if(!conn.exists(key)){
            ZParams params = new ZParams().aggregate(ZParams.Aggregate.MAX); // 交集时取相同元素的分值最大的那个
            conn.zinterstore(key, params, "group:" + groupId, order);
            conn.expire(key, 60); // 缓存
        }
        return getArticles(conn, page, key);
    }
}
