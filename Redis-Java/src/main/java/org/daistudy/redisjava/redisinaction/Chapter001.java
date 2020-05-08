package org.daistudy.redisjava.redisinaction;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ZParams;

import java.util.*;

/**
 * 需求：使用 Redis 构建一个简单的文章投票网站的后端
 * 网站根据文章的发布时间和文章获得的投票数量计算出一个评分，按照评分决定如何排序和展示文章
 *
 * 评分可以随着时间流逝而不断减少（计算方法）：文章得到的支持票数乘以一个常量 + 文章的发布时间 == 文章的评分
 * 从Unix时间（UTC时区1970年1月1日）到现在为止经过的秒数来计算文章的评分
 *
 * 文章信息使用 Redis 中的 Hash 结构存储 -- hash article:articleId 默认投票数量为0
 * 根据发布时间排序文章 -- zset time:
 * 根据评分排序文章 -- zset score:
 * 根据发布时间排序群组文章 -- zset time:groupId == time: 和 group:groupId 的 zinterstore
 * 根据评分排序群组文章 -- zset score:groupId == score: 和 group:groupId 的 zinterstore
 * 每篇文章的已投票用户名单（不能重复投票） -- set voted:articleId
 * 用户 -- user:userId
 * 用户集合 -- set user:list
 * 文章集合 -- set article:list
 * 文章ID的计数器 -- string article:
 * 用户ID的计数器 -- string user:
 * 群组ID的计数器 -- string group:
 * 同属一个群组中的文章ID记录集合 -- set group:groupId
 * 规定当一篇文章发布期满一周之后，用户将不能再对其进行投票，文章的评分将被固定下来，而文章的已投票用户名单的集合也会被删除
 *
 * 群组存放文章
 */
public class Chapter001 {
    private static final int ONE_WEEK_IN_SECONDS = 7 * 86400;
    private static final int VOTE_SCORE = 432;
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

            String articleId = postArticle(
                    conn, userRandom, "A title", "http://www.google.com");
            System.out.println(userRandom + "发布了一篇文章，ID号为: " + articleId);
            System.out.println("文章内容如下:");
            Map<String,String> articleData = conn.hgetAll("article:" + articleId);
            for (Map.Entry<String,String> entry : articleData.entrySet()){
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            }

            System.out.println();
            i++;
        }


        count = random.nextInt(1000);
        i = 0;
        while(i < count){
            userRandom = conn.srandmember("user:list");
            articleRandom = conn.srandmember("article:list");
            articleVote(conn, userRandom, articleRandom);
            String votes = conn.hget(articleRandom, "votes");
            System.out.println("有人给文章" + articleRandom + "投票了，现在票数是： " + votes);
            assert Integer.parseInt(votes) > 1;
            i++;
        }


        System.out.println("目前评分最高的文章列表:");
        List<Map<String,String>> articles = getArticles(conn, 1);
        printArticles(articles);
        assert articles.size() >= 1;


//        addArticleToGroups(conn, articleId, new String[]{"new-group"});
//        System.out.println("We added the article to a new group, other articles include:");
//        articles = getGroupArticles(conn, "new-group", 1);
//        printArticles(articles);
//        assert articles.size() >= 1;
    }

    private void printArticles(List<Map<String, String>> articles) {
        for (Map<String,String> article : articles){
            System.out.println("  id: " + article.get("id"));
            for (Map.Entry<String,String> entry : article.entrySet()){
                if (entry.getKey().equals("id")){
                    continue;
                }
                System.out.println("    " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }

    public Jedis init(){
        Jedis conn = new Jedis("localhost");
        conn.select(15);
        conn.flushDB();
        generateUsers(conn);
        generateGroups(conn);
        return conn;
    }

    public void generateUsers(Jedis conn){
        genereteSet(conn, "user:");
    }

    public void generateGroups(Jedis conn){
        genereteSet(conn, "group:");
    }

    private void genereteSet(Jedis conn, String key){
        if(!conn.exists(key)){
            conn.set(key, "100000");
        }
        // 随机递增（1-101）生成1000个
        Random random = new Random();
        int i = 0;
        while(i < 1000){
            int incr = random.nextInt(100) + 1;
            conn.incrBy(key, incr);
            conn.sadd(key + "list", key + conn.get(key));
            i++;
        }
    }

    /**
     * user 用户给 article 文章投票
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
     * @param conn Redis
     * @param user 发布人
     * @param title 发布文章的标题
     * @param link 发布文章的链接
     */
    public String postArticle(Jedis conn, String user, String title, String link){
        // 1.1 生成文章ID
        String articleId;
        if (conn.exists("article:")) {
            articleId = conn.incrBy("article:", new Random().nextInt(1000) + 1).toString();
        }else{
            articleId = "100000";
            conn.set("article:", articleId);
        }
        String article = "article:" + articleId;

        // 1.2 将文章添加到文章集合中
        conn.sadd("article:list", article);

        // 2. 生成文章已投票用户名单
        String voted = "voted:" + articleId;
//        conn.sadd(voted, user); // 是否需要默认发布人给自己发布文章投一票
        conn.expire(voted, ONE_WEEK_IN_SECONDS); // 集合的键不存在，是否可以设置过期时间？？？？

        // 3. 设置文章信息
        long now = System.currentTimeMillis() / 1000;
        Map<String, String> articleData = new HashMap<>();
        articleData.put("id", articleId);
        articleData.put("title", title);
        articleData.put("link", link);
        articleData.put("user", user);
        articleData.put("now", String.valueOf(now));
        articleData.put("votes", "0"); // 是否需要默认发布人给自己发布文章投一票
        conn.hmset(article, articleData);

        // 4. 设置文章评分
        conn.zadd("score:", now + VOTE_SCORE, article);

        // 5. 设置文章时间
        conn.zadd("time:", now, article);

        return articleId;
    }

    public List<Map<String, String>> getArticles(Jedis conn, int page){
        return getArticles(conn, page, "score:");
    }

    public List<Map<String, String>> getArticles(Jedis conn, int page, String order){
        // 1. 获取第 page 页的文章ID
        int start = (page - 1) * ARTICLES_PER_PAGE;
        int end = start + ARTICLES_PER_PAGE - 1;
        Set<String> articleIds = conn.zrevrange(order, start, end);

        // 2. 填充文章信息
        List<Map<String, String>> articles = new ArrayList<>();
        for (String articleId : articleIds) {
            Map<String, String> articleData = conn.hgetAll(articleId);
            articleData.put("id", articleId);
            articles.add(articleData);
        }
        return articles;
    }

    /**
     * 将文章添加到群组中
     * @param conn Redis
     * @param articleId 文章 -- 应该传入 articleId 还是 article:articleId
     * @param toAdd 群组
     */
    public void addArticleToGroups(Jedis conn, String articleId, String[] toAdd){
        String article = "article:" + articleId;
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
